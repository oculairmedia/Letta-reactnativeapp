package com.letta.mobile.feature.chat

import com.letta.mobile.data.a2ui.A2uiSurfaceManager
import com.letta.mobile.data.model.UiMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class A2uiHistoryExtractorTest {
    @Test
    fun `extract strips historical tags and replays surface messages`() {
        val extraction = A2uiHistoryExtractor.extract(
            listOf(
                message(
                    content = """
                    Before
                    <a2ui-json>
                    [
                      {"version":"v0.9","createSurface":{"surfaceId":"surface-1","catalogId":"basic"}},
                      {"version":"v0.9","updateComponents":{"surfaceId":"surface-1","root":"title","components":[
                        {"id":"title","component":"Text","text":{"literalString":"Recovered surface"}}
                      ]}}
                    ]
                    </a2ui-json>
                    After
                    """.trimIndent(),
                )
            )
        )

        assertFalse(extraction.messages.single().content.contains("<a2ui-json>"))
        assertFalse(extraction.messages.single().content.contains("Recovered surface"))
        assertTrue(extraction.messages.single().content.contains("Before"))
        assertEquals(2, extraction.a2uiMessages.size)

        val manager = A2uiSurfaceManager()
        manager.applyMessages(extraction.a2uiMessages)
        assertEquals("title", manager.surface("surface-1")!!.rootComponentId)
    }

    @Test
    fun `extract preserves deleteSurface order so deleted history does not resurrect`() {
        val extraction = A2uiHistoryExtractor.extract(
            listOf(
                message(
                    content = """
                    <a2ui-json>
                    [
                      {"version":"v0.9","createSurface":{"surfaceId":"surface-1","catalogId":"basic"}},
                      {"version":"v0.9","deleteSurface":{"surfaceId":"surface-1"}}
                    ]
                    </a2ui-json>
                    """.trimIndent(),
                )
            )
        )

        val manager = A2uiSurfaceManager()
        manager.applyMessages(extraction.a2uiMessages)

        assertNull(manager.surface("surface-1"))
    }

    private fun message(content: String): UiMessage = UiMessage(
        id = "msg-1",
        role = "assistant",
        content = content,
        timestamp = "2026-05-28T00:00:00Z",
    )
}
