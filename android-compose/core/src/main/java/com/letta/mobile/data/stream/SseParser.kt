package com.letta.mobile.data.stream

import com.letta.mobile.data.model.LettaMessage
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

object SseParser {
    private data class ProcessedEvent(
        val message: LettaMessage? = null,
        val isDone: Boolean = false,
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parse(channel: ByteReadChannel): Flow<LettaMessage> = flow {
        val buffer = StringBuilder()
        val lineReader = Utf8LineReader(channel)
        var isDone = false

        while (!isDone) {
            val line = lineReader.readLine() ?: break

            if (line.isEmpty()) {
                val event = buffer.toString()
                buffer.clear()

                if (event.isNotBlank()) {
                    val processed = processEvent(event)
                    if (processed.isDone) {
                        isDone = true
                    } else {
                        processed.message?.let { emit(it) }
                    }
                }
            } else {
                buffer.append(line).append("\n")
            }
        }

        if (!isDone && buffer.isNotBlank()) {
            processEvent(buffer.toString()).message?.let { emit(it) }
        }
    }

    private fun processEvent(event: String): ProcessedEvent {
        val lines = event.lines()

        for (line in lines) {
            if (line.startsWith("data: ")) {
                val data = line.substring(6).trim()

                if (data == "[DONE]") {
                    return ProcessedEvent(isDone = true)
                }

                if (data.startsWith("{")) {
                    return try {
                        val message = json.decodeFromString<LettaMessage>(data)
                        if (message.messageType == "ping") ProcessedEvent() else ProcessedEvent(message = message)
                    } catch (e: Exception) {
                        android.util.Log.w("SseParser", "Failed to parse SSE event: ${data.take(100)}", e)
                        ProcessedEvent()
                    }
                }
            }
        }

        return ProcessedEvent()
    }
}
