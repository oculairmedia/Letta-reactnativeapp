package com.letta.mobile.bot.protocol

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI

internal const val WS_PATH = "/api/v1/agent-gateway"

internal fun normalizeWebSocketUrl(
    baseUrl: String,
    progressiveToolCalls: Boolean = false,
): String {
    val uri = URI(baseUrl.trim())
    val scheme = when (uri.scheme?.lowercase()) {
        "http" -> "ws"
        "https" -> "wss"
        "ws", "wss" -> uri.scheme.lowercase()
        else -> throw IllegalArgumentException("Unsupported baseUrl scheme: ${uri.scheme}")
    }
    val path = uri.path.orEmpty().trimEnd('/').let { currentPath ->
        if (currentPath.endsWith(WS_PATH)) currentPath else "$currentPath$WS_PATH"
    }
    val mergedQuery = mergeQuery(uri.query, progressiveToolCalls)
    return URI(scheme, uri.userInfo, uri.host, uri.port, path, mergedQuery, uri.fragment).toString()
}

internal fun mergeQuery(existing: String?, progressiveToolCalls: Boolean): String? {
    if (!progressiveToolCalls) return existing
    val flag = "progressive_tool_calls=1"
    if (existing.isNullOrBlank()) return flag
    val parts = existing.split('&')
    val alreadyPresent = parts.any { it.equals(flag, ignoreCase = true) ||
        it.startsWith("progressive_tool_calls=", ignoreCase = true) }
    return if (alreadyPresent) existing else "$existing&$flag"
}

internal fun normalizeHttpBaseUrl(baseUrl: String): String {
    val uri = URI(baseUrl.trim())
    val scheme = when (uri.scheme?.lowercase()) {
        "ws" -> "http"
        "wss" -> "https"
        "http", "https" -> uri.scheme.lowercase()
        else -> throw IllegalArgumentException("Unsupported baseUrl scheme: ${uri.scheme}")
    }
    val path = uri.path.orEmpty().trimEnd('/').removeSuffix(WS_PATH).ifBlank { "" }
    return URI(scheme, uri.userInfo, uri.host, uri.port, path.ifBlank { null }, null, null).toString().trimEnd('/')
}

internal fun parseIncoming(json: Json, text: String): WsInboundMessage {
    val element = json.parseToJsonElement(text)
    val obj = element as? JsonObject ?: throw SerializationException("Expected JSON object")
    val type = obj["type"]?.jsonPrimitive?.content ?: throw SerializationException("Missing type")

    return when (type) {
        "session_init" -> json.decodeFromJsonElement(WsSessionInit.serializer(), element)
        "stream" -> json.decodeFromJsonElement(WsStreamEventMessage.serializer(), element)
        "result" -> json.decodeFromJsonElement(WsResultMessage.serializer(), element)
        "error" -> json.decodeFromJsonElement(WsErrorMessage.serializer(), element)
        else -> throw SerializationException("Unknown WebSocket message type: $type")
    }
}
