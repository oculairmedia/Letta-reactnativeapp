package com.letta.mobile.ui.screens.chat

import com.letta.mobile.bot.protocol.BotStreamChunk
import com.letta.mobile.bot.protocol.BotStreamEvent
import com.letta.mobile.data.model.ToolCall
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.jupiter.api.Tag

@Tag("unit")
class ClientModeStreamReducerTest {
    private var now = 1_000L
    private val reducer = ClientModeStreamReducer(nowMs = { now })

    @Test
    fun `assistant deltas append to one legacy bubble`() = runTest {
        val first = reducer.reduceLegacy(
            state = ClientModeStreamReducerState(),
            chunk = BotStreamChunk(text = "Hel"),
            assistantMessageId = "assistant-1",
            timestamp = "t0",
        )
        val second = reducer.reduceLegacy(
            state = first,
            chunk = BotStreamChunk(text = "lo"),
            assistantMessageId = "assistant-1",
            timestamp = "t1",
        )

        assertEquals(1, second.messages.size)
        assertEquals("assistant-1", second.messages.single().id)
        assertEquals("Hello", second.messages.single().content)
        assertEquals("t0", second.messages.single().timestamp)
    }

    @Test
    fun `reasoning deltas append and mark reasoning`() = runTest {
        val first = reducer.reduceLegacy(
            state = ClientModeStreamReducerState(),
            chunk = BotStreamChunk(event = BotStreamEvent.REASONING, uuid = "reason-1", text = "Think"),
            assistantMessageId = "assistant-1",
            timestamp = "t0",
        )
        val second = reducer.reduceLegacy(
            state = first,
            chunk = BotStreamChunk(event = BotStreamEvent.REASONING, uuid = "reason-1", text = "ing"),
            assistantMessageId = "assistant-1",
            timestamp = "t1",
        )

        assertEquals("Thinking", second.messages.single().content)
        assertEquals(true, second.messages.single().isReasoning)
        assertEquals("t0", second.messages.single().timestamp)
    }

    @Test
    fun `tool call snapshot creates compact tool card and result merges by call id`() = runTest {
        val call = reducer.reduceLegacy(
            state = ClientModeStreamReducerState(),
            chunk = BotStreamChunk(
                event = BotStreamEvent.TOOL_CALL,
                toolCallId = "call-1",
                toolName = "shell",
                toolInput = JsonPrimitive("ls"),
            ),
            assistantMessageId = "assistant-1",
            timestamp = "t0",
        )
        now = 1_250L
        val result = reducer.reduceLegacy(
            state = call,
            chunk = BotStreamChunk(
                event = BotStreamEvent.TOOL_RESULT,
                toolCallId = "call-1",
                text = "ok",
            ),
            assistantMessageId = "assistant-1",
            timestamp = "t1",
        )

        val tool = result.messages.single().toolCalls?.single()
        assertNotNull(tool)
        assertEquals("shell", tool?.name)
        assertEquals("\"ls\"", tool?.arguments)
        assertEquals("ok", tool?.result)
        assertEquals("success", tool?.status)
        assertEquals(250L, tool?.executionTimeMs)
        assertEquals("client-tool-call-1", result.messages.single().id)
    }

    @Test
    fun `batched tool calls share one message and individual results merge`() = runTest {
        val call = reducer.reduceLegacy(
            state = ClientModeStreamReducerState(),
            chunk = BotStreamChunk(
                event = BotStreamEvent.TOOL_CALL,
                toolCallId = "batch-1",
                toolCalls = listOf(
                    ToolCall(toolCallId = "call-a", name = "read", arguments = "a"),
                    ToolCall(toolCallId = "call-b", name = "write", arguments = "b"),
                ),
            ),
            assistantMessageId = "assistant-1",
            timestamp = "t0",
        )
        now = 1_500L
        val result = reducer.reduceLegacy(
            state = call,
            chunk = BotStreamChunk(
                event = BotStreamEvent.TOOL_RESULT,
                toolCallId = "call-b",
                text = "done",
            ),
            assistantMessageId = "assistant-1",
            timestamp = "t1",
        )

        assertEquals("batch-1", result.toolBatchMessageIds["call-a"])
        assertEquals("batch-1", result.toolBatchMessageIds["call-b"])
        assertEquals(1, result.messages.size)
        assertEquals("client-tool-batch-1", result.messages.single().id)
        val tools = result.messages.single().toolCalls.orEmpty()
        assertEquals(listOf("read", "write"), tools.map { it.name })
        assertEquals(null, tools[0].result)
        assertEquals("done", tools[1].result)
        assertEquals(500L, tools[1].executionTimeMs)
    }

    @Test
    fun `tool result arriving before tool call still renders result card`() = runTest {
        val state = reducer.reduceLegacy(
            state = ClientModeStreamReducerState(),
            chunk = BotStreamChunk(
                event = BotStreamEvent.TOOL_RESULT,
                toolCallId = "call-late",
                toolName = "tool",
                text = "late result",
            ),
            assistantMessageId = "assistant-1",
            timestamp = "t0",
        )

        val tool = state.messages.single().toolCalls?.single()
        assertEquals("client-tool-call-late", state.messages.single().id)
        assertEquals("late result", tool?.result)
        assertEquals("success", tool?.status)
    }
}
