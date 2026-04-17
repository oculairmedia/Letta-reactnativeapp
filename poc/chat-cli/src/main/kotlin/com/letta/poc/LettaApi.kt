package com.letta.poc

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

/**
 * Minimal Letta API client for POC.
 *
 * Intentionally simple: no retries, no caching, no fancy error handling.
 * The goal is to exercise the Timeline architecture, not build a production client.
 */
class LettaApi(
    private val baseUrl: String,
    private val token: String,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        prettyPrint = false
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 120_000
        }
        expectSuccess = false
    }

    /** Create a new conversation for an agent. */
    suspend fun createConversation(agentId: String, name: String = "poc-session"): String {
        val response = client.post("$baseUrl/v1/conversations") {
            url { parameters.append("agent_id", agentId) }
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("name", name) })
        }
        if (!response.status.isSuccess()) {
            error("createConversation failed: ${response.status} ${response.bodyAsText()}")
        }
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        return body["id"]!!.jsonPrimitive.content
    }

    /** GET /v1/conversations/{id}/messages — fetch stored messages. */
    suspend fun listMessages(
        conversationId: String,
        limit: Int = 50,
        order: String = "asc",
        after: String? = null,
    ): List<ServerMessage> {
        val response = client.get("$baseUrl/v1/conversations/$conversationId/messages") {
            header("Authorization", "Bearer $token")
            url {
                parameters.append("limit", limit.toString())
                parameters.append("order", order)
                if (after != null) parameters.append("after", after)
            }
        }
        if (!response.status.isSuccess()) {
            error("listMessages failed: ${response.status} ${response.bodyAsText()}")
        }
        val arr = json.parseToJsonElement(response.bodyAsText()).jsonArray
        return arr.map { parseServerMessage(it.jsonObject) }
    }

    /**
     * Stream a message send. Yields each parsed SSE event.
     * The returned Flow completes when the stream ends.
     */
    fun streamSend(
        conversationId: String,
        content: String,
        otid: String,
        includeTypes: List<String> = listOf("assistant_message", "reasoning_message", "tool_call_message", "tool_return_message"),
    ): Flow<StreamEvent> = flow {
        val body = buildJsonObject {
            putJsonArray("messages") {
                addJsonObject {
                    put("type", "message")
                    put("role", "user")
                    put("content", content)
                    put("otid", otid)
                }
            }
            put("streaming", true)
            putJsonArray("include_return_message_types") {
                includeTypes.forEach { add(it) }
            }
        }

        val statement = client.preparePost("$baseUrl/v1/conversations/$conversationId/messages") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(body)
        }

        statement.execute { response ->
            if (!response.status.isSuccess()) {
                error("streamSend failed: ${response.status} ${response.bodyAsText()}")
            }
            val channel: ByteReadChannel = response.bodyAsChannel()
            val buffer = StringBuilder()
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break
                if (line.isBlank()) {
                    // Dispatch accumulated event
                    val data = buffer.toString()
                    buffer.clear()
                    if (data.isNotBlank()) {
                        parseSseEvent(data)?.let { emit(it) }
                    }
                } else if (line.startsWith("data: ")) {
                    buffer.appendLine(line.removePrefix("data: "))
                } else if (line.startsWith("event: ")) {
                    // event type marker — stored for context but not required for our parse
                }
            }
            // Flush any remaining buffer
            if (buffer.isNotEmpty()) {
                parseSseEvent(buffer.toString())?.let { emit(it) }
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun parseSseEvent(data: String): StreamEvent? {
        return try {
            val obj = json.parseToJsonElement(data).jsonObject
            val type = obj["message_type"]?.jsonPrimitive?.contentOrNull ?: return null
            when (type) {
                "ping" -> StreamEvent.Ping(obj["run_id"]?.jsonPrimitive?.contentOrNull)
                "stop_reason" -> StreamEvent.StopReason(
                    obj["stop_reason"]?.jsonPrimitive?.contentOrNull ?: "unknown"
                )
                "error_message" -> StreamEvent.Error(
                    obj["error_type"]?.jsonPrimitive?.contentOrNull ?: "unknown",
                    obj["message"]?.jsonPrimitive?.contentOrNull ?: "",
                    obj["detail"]?.jsonPrimitive?.contentOrNull,
                )
                else -> StreamEvent.Message(parseServerMessage(obj))
            }
        } catch (e: Exception) {
            System.err.println("parseSseEvent failed for: $data — $e")
            null
        }
    }

    private fun parseServerMessage(obj: JsonObject): ServerMessage {
        val content = when (val c = obj["content"]) {
            is JsonPrimitive -> c.content
            is JsonArray -> c.joinToString("") {
                (it as? JsonObject)?.get("text")?.jsonPrimitive?.contentOrNull ?: ""
            }
            else -> ""
        }
        return ServerMessage(
            id = obj["id"]?.jsonPrimitive?.contentOrNull ?: "",
            messageType = obj["message_type"]?.jsonPrimitive?.contentOrNull ?: "unknown",
            otid = obj["otid"]?.jsonPrimitive?.contentOrNull,
            content = content,
            date = obj["date"]?.jsonPrimitive?.contentOrNull ?: "",
            runId = obj["run_id"]?.jsonPrimitive?.contentOrNull,
            stepId = obj["step_id"]?.jsonPrimitive?.contentOrNull,
        )
    }

    fun close() = client.close()
}

@Serializable
data class ServerMessage(
    val id: String,
    val messageType: String,
    val otid: String?,
    val content: String,
    val date: String,
    val runId: String?,
    val stepId: String?,
)

sealed class StreamEvent {
    data class Ping(val runId: String?) : StreamEvent()
    data class Message(val message: ServerMessage) : StreamEvent()
    data class StopReason(val reason: String) : StreamEvent()
    data class Error(val type: String, val message: String, val detail: String?) : StreamEvent() {
        /** cleanup_error with "uuid" detail is a known benign server bug. */
        val isBenignCleanupError: Boolean
            get() = type == "cleanup_error" && detail?.contains("uuid") == true
    }
}
