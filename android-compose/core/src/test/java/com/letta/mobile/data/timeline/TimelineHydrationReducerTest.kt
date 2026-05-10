package com.letta.mobile.data.timeline

import com.letta.mobile.data.model.ToolCall
import com.letta.mobile.data.model.ToolCallMessage
import com.letta.mobile.data.model.ToolReturnMessage
import com.letta.mobile.data.model.UserMessage
import java.time.Instant
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineHydrationReducerTest {

    @Test
    fun `reduce attaches tool returns and drops standalone return bubbles`() {
        val result = TimelineHydrationReducer.reduce(
            conversationId = "conversation-1",
            serverMessagesChronological = listOf(
                ToolCallMessage(
                    id = "tool-call-1",
                    toolCall = ToolCall(toolCallId = "call-1", name = "Bash", arguments = "{}"),
                ),
                ToolReturnMessage(
                    id = "tool-return-1",
                    toolCallId = "call-1",
                    status = "success",
                    toolReturnRaw = JsonPrimitive("ok"),
                ),
            ),
            timelineBeforeFetch = Timeline("conversation-1"),
            currentTimeline = Timeline("conversation-1"),
            diskRecords = emptyList(),
        )

        assertEquals(1, result.visibleEventCount)
        val toolEvent = result.timeline.events.single() as TimelineEvent.Confirmed
        assertEquals(TimelineMessageType.TOOL_CALL, toolEvent.messageType)
        assertEquals("ok", toolEvent.toolReturnContentByCallId["call-1"])
        assertTrue(toolEvent.approvalDecided)
    }

    @Test
    fun `reduce preserves concurrent locals after server snapshot`() {
        val local = TimelineEvent.Local(
            position = 1.0,
            otid = "local-1",
            content = "pending",
            role = Role.USER,
            sentAt = Instant.parse("2026-05-10T00:00:00Z"),
            deliveryState = DeliveryState.SENDING,
        )

        val result = TimelineHydrationReducer.reduce(
            conversationId = "conversation-1",
            serverMessagesChronological = listOf(UserMessage(id = "server-1", contentRaw = JsonPrimitive("confirmed"))),
            timelineBeforeFetch = Timeline("conversation-1"),
            currentTimeline = Timeline("conversation-1", events = listOf(local)),
            diskRecords = emptyList(),
        )

        assertEquals(listOf("server-server-1-user", "local-1"), result.timeline.events.map { it.otid })
        assertEquals(1.0, result.timeline.events.first().position, 0.0)
        assertEquals(2.0, result.timeline.events.last().position, 0.0)
    }
}
