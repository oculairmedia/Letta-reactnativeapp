package com.letta.mobile.bot.protocol

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

internal fun BotChatRequest.outboundContent(json: Json): String =
    contentItems?.takeIf { it.isNotEmpty() }?.let { json.encodeToString(ListSerializer(BotMessageContentItem.serializer()), it) }
        ?: message

internal fun List<BotStreamChunk>.collectFinalResponse(): BotChatResponse {
    val assistantText = buildString {
        for (chunk in this@collectFinalResponse) {
            if (chunk.done) continue
            if (chunk.event == null || chunk.event == BotStreamEvent.ASSISTANT) {
                append(chunk.text.orEmpty())
            }
        }
    }
    val terminal = lastOrNull { it.done } ?: lastOrNull()
    return BotChatResponse(
        response = assistantText,
        conversationId = terminal?.conversationId,
        agentId = terminal?.agentId,
    )
}

internal fun String.toGatewayErrorCode(): BotGatewayErrorCode =
    runCatching { BotGatewayErrorCode.valueOf(this) }.getOrDefault(BotGatewayErrorCode.STREAM_ERROR)

internal fun WsStreamEventMessage.toChunk(
    conversationId: String?,
    agentId: String?,
): BotStreamChunk = BotStreamChunk(
    text = content,
    conversationId = this.conversationId ?: conversationId,
    agentId = agentId,
    event = event,
    toolName = toolName,
    toolCallId = toolCallId,
    toolInput = toolInput,
    toolCalls = toolCalls,
    isError = isError,
    requestId = requestId,
    uuid = uuid,
    oldConversationId = oldConversationId,
    done = false,
)
