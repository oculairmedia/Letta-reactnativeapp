package com.letta.mobile.testutil

import com.letta.mobile.data.model.MessageContentPart
import com.letta.mobile.data.timeline.TimelineEvent
import com.letta.mobile.data.timeline.api.TimelineClientModeWriter

/** Deterministic fake for notification-reply Client Mode timeline writes. */
class FakeTimelineClientModeWriter : TimelineClientModeWriter {
    val clientModeLocals: MutableList<ClientModeLocal> = mutableListOf()
    val assistantChunks: MutableList<AssistantChunk> = mutableListOf()
    val collapsedConversationIds: MutableList<String> = mutableListOf()
    private val assistantLocalsById: MutableMap<String, TimelineEvent.Local> = mutableMapOf()

    override suspend fun appendClientModeLocal(
        conversationId: String,
        content: String,
        attachments: List<MessageContentPart.Image>,
    ): String {
        val localId = "local-${clientModeLocals.size + 1}"
        clientModeLocals += ClientModeLocal(conversationId, content, attachments, localId)
        return localId
    }

    override suspend fun upsertClientModeLocalAssistantChunk(
        conversationId: String,
        localId: String,
        build: () -> TimelineEvent.Local,
        transform: (TimelineEvent.Local) -> TimelineEvent.Local,
    ): String {
        val previous = assistantLocalsById[localId]
        val next = if (previous == null) build() else transform(previous)
        assistantLocalsById[localId] = next
        assistantChunks += AssistantChunk(conversationId, localId, next)
        return localId
    }

    override suspend fun postHandlerCollapse(conversationId: String) {
        collapsedConversationIds += conversationId
    }

    data class ClientModeLocal(
        val conversationId: String,
        val content: String,
        val attachments: List<MessageContentPart.Image>,
        val localId: String,
    )

    data class AssistantChunk(
        val conversationId: String,
        val localId: String,
        val event: TimelineEvent.Local,
    )
}
