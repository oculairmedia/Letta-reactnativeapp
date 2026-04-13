package com.letta.mobile.bot.protocol

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

object BotSseParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parse(channel: ByteReadChannel): Flow<BotStreamChunk> = flow {
        val buffer = StringBuilder()

        while (true) {
            val line = channel.readUTF8Line() ?: break
            if (line.isEmpty()) {
                val event = buffer.toString()
                buffer.clear()
                if (event.isNotBlank()) {
                    val chunk = processEvent(event) ?: continue
                    emit(chunk)
                    if (chunk.done) {
                        break
                    }
                }
            } else {
                buffer.append(line).append('\n')
            }
        }

        if (buffer.isNotBlank()) {
            processEvent(buffer.toString())?.let { emit(it) }
        }
    }

    private fun processEvent(event: String): BotStreamChunk? {
        for (line in event.lines()) {
            if (!line.startsWith("data: ")) {
                continue
            }

            val data = line.removePrefix("data: ").trim()
            if (data == "[DONE]") {
                return BotStreamChunk(done = true)
            }

            if (!data.startsWith("{")) {
                continue
            }

            return try {
                json.decodeFromString<BotStreamChunk>(data)
            } catch (_: Exception) {
                null
            }
        }

        return null
    }
}
