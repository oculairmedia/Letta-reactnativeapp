package com.letta.mobile.data.transport

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Test

private const val PING_TEST_TIMEOUT_MS = 2_000L

class ChannelTransportPingTest {
    @Test
    fun `responds to shim ping with app-level pong`() = runBlocking {
        val shim = PingShimServer()
        val transport = ChannelTransport(RunCursorStore.inMemory())

        try {
            transport.connect(
                baseShimUrl = shim.baseUrl(),
                token = "token",
                deviceId = "device",
                clientVersion = "test",
            )

            withTimeout(PING_TEST_TIMEOUT_MS) {
                transport.state.first { it is ChannelTransport.State.Connected }
            }

            val pong = shim.frames.receiveType("pong")

            assertEquals("pong", pong.stringValue("type"))
            assertEquals("ping-1", pong.stringValue("id"))
        } finally {
            transport.disconnect()
            shim.close()
        }
    }

    private class PingShimServer {
        private val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
        private val server = MockWebServer()
        val frames = Channel<JsonObject>(Channel.UNLIMITED)

        init {
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse =
                    MockResponse().withWebSocketUpgrade(
                        object : WebSocketListener() {
                            override fun onMessage(webSocket: WebSocket, text: String) {
                                val frame = json.parseToJsonElement(text).jsonObject
                                frames.trySend(frame)
                                if (frame.stringValue("type") == "hello") {
                                    webSocket.send(welcomeFrame())
                                    webSocket.send(pingFrame())
                                }
                            }

                            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                                webSocket.close(code, reason)
                            }

                            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) = Unit
                        }
                    )
            }
            server.start()
        }

        fun baseUrl(): String = server.url("/").toString().removeSuffix("/")

        fun close() {
            frames.close()
            server.shutdown()
        }

        private fun welcomeFrame(): String =
            """
            {"v":1,"type":"welcome","id":"welcome-1","ts":"2026-05-27T00:00:00Z",
             "server_id":"server","session_id":"session"}
            """.trimIndent()

        private fun pingFrame(): String =
            """
            {"v":1,"type":"ping","id":"ping-1","ts":"2026-05-27T00:00:01Z"}
            """.trimIndent()
    }

}

private suspend fun Channel<JsonObject>.receiveType(type: String): JsonObject =
    withTimeout(PING_TEST_TIMEOUT_MS) {
        while (true) {
            val frame = receive()
            if (frame.stringValue("type") == type) return@withTimeout frame
        }
        error("unreachable")
    }

private fun JsonObject.stringValue(key: String): String? =
    this[key]?.jsonPrimitive?.contentOrNull
