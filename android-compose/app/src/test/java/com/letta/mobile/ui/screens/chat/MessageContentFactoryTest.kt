package com.letta.mobile.ui.screens.chat

import com.letta.mobile.data.model.UiToolCall
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageContentFactoryTest {

    @Test
    fun `tool call entrance animation is limited to active streaming`() {
        assertTrue(shouldAnimateToolCallEntrance(isStreaming = true))
        assertFalse(shouldAnimateToolCallEntrance(isStreaming = false))
    }

    @Test
    fun `multiple tool calls use compact group renderer`() {
        assertFalse(shouldUseCompactToolCallGroup(emptyList()))
        assertFalse(
            shouldUseCompactToolCallGroup(
                listOf(UiToolCall(name = "Bash", arguments = "{}", result = null))
            )
        )
        assertTrue(
            shouldUseCompactToolCallGroup(
                listOf(
                    UiToolCall(name = "Bash", arguments = "{\"command\":\"a\"}", result = null),
                    UiToolCall(name = "Bash", arguments = "{\"command\":\"b\"}", result = null),
                )
            )
        )
    }
}
