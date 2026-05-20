package com.letta.mobile.data.timeline.api

import com.letta.mobile.data.model.MessageContentPart
import com.letta.mobile.data.timeline.TimelineEvent

/**
 * Narrow timeline surface used by Client Mode notification-reply writers.
 * Tests can provide hand-written fakes without constructing the stateful
 * TimelineRepository registry and its background sync loops.
 */
interface TimelineClientModeWriter {
    suspend fun appendClientModeLocal(
        conversationId: String,
        content: String,
        attachments: List<MessageContentPart.Image> = emptyList(),
    ): String

    suspend fun upsertClientModeLocalAssistantChunk(
        conversationId: String,
        localId: String,
        build: () -> TimelineEvent.Local,
        transform: (TimelineEvent.Local) -> TimelineEvent.Local,
    ): String

    suspend fun postHandlerCollapse(conversationId: String)
}
