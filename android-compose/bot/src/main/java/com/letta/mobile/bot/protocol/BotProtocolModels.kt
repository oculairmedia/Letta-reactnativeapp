package com.letta.mobile.bot.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BotChatRequest(
    val message: String,
    @SerialName("channel_id") val channelId: String? = null,
    @SerialName("chat_id") val chatId: String? = null,
    @SerialName("sender_id") val senderId: String? = null,
    @SerialName("sender_name") val senderName: String? = null,
    @SerialName("agent_id") val agentId: String? = null,
    @SerialName("conversation_id") val conversationId: String? = null,
)

@Serializable
data class BotChatResponse(
    val response: String,
    @SerialName("conversation_id") val conversationId: String? = null,
    @SerialName("agent_id") val agentId: String? = null,
)

@Serializable
data class BotStatusResponse(
    val status: String,
    val agents: List<String>,
)

@Serializable
data class BotAgentInfo(
    val id: String,
    val name: String,
    val status: String,
)

@Serializable
data class BotStreamChunk(
    val text: String? = null,
    @SerialName("conversation_id") val conversationId: String? = null,
    @SerialName("agent_id") val agentId: String? = null,
    val done: Boolean = false,
)

@Serializable
data class BotErrorResponse(
    val error: String,
)
