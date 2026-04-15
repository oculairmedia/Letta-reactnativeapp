package com.letta.mobile.chat.model

import androidx.compose.runtime.Immutable

enum class MessageRole { User, Assistant, Tool, System }

enum class MessageStatus { Sent, Streaming, Complete, Error }

@Immutable
data class ChatMessage(
    val id: String,
    val role: MessageRole,
    val content: String,
    val timestamp: String,
    val status: MessageStatus = MessageStatus.Complete,
    val isPending: Boolean = false,
    val isReasoning: Boolean = false,
    val toolCalls: List<ChatToolCall>? = null,
) {
    fun contentHash(): String = "${role.name}:${content.hashCode()}"
}

@Immutable
data class ChatToolCall(
    val id: String = "",
    val name: String,
    val arguments: String = "",
    val result: String? = null,
    val isPending: Boolean = false,
)

@Immutable
data class ChatConversation(
    val id: String,
    val agentId: String,
    val summary: String? = null,
    val lastMessageAt: String? = null,
    val createdAt: String? = null,
)
