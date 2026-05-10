package com.letta.mobile.ui.screens.chat

import com.letta.mobile.bot.protocol.BotStreamChunk
import com.letta.mobile.bot.protocol.BotStreamEvent
import com.letta.mobile.data.model.ToolCall
import com.letta.mobile.data.model.UiMessage
import com.letta.mobile.data.model.UiToolCall

internal class ClientModeStreamReducer(
    private val nowMs: () -> Long = System::currentTimeMillis,
) {
    fun reduceLegacy(
        state: ClientModeStreamReducerState,
        chunk: BotStreamChunk,
        assistantMessageId: String,
        timestamp: String,
    ): ClientModeStreamReducerState {
        return when (chunk.event) {
            BotStreamEvent.REASONING -> reduceReasoning(state, chunk, assistantMessageId, timestamp)
            BotStreamEvent.TOOL_CALL,
            BotStreamEvent.TOOL_RESULT -> reduceToolFrame(state, chunk, timestamp)
            else -> reduceAssistantDelta(state, chunk, assistantMessageId, timestamp)
        }
    }

    private fun reduceReasoning(
        state: ClientModeStreamReducerState,
        chunk: BotStreamChunk,
        assistantMessageId: String,
        timestamp: String,
    ): ClientModeStreamReducerState {
        val messageId = chunk.uuid ?: "client-reasoning-$assistantMessageId"
        val delta = chunk.text.orEmpty()
        return state.upsertMessage(messageId, timestamp) { existing ->
            val prior = existing?.content.orEmpty()
            val merged = if (delta.isEmpty()) prior else prior + delta
            (existing ?: UiMessage(
                id = messageId,
                role = "assistant",
                content = "",
                timestamp = timestamp,
                isReasoning = true,
            )).copy(
                content = merged,
                isReasoning = true,
            )
        }
    }

    private fun reduceAssistantDelta(
        state: ClientModeStreamReducerState,
        chunk: BotStreamChunk,
        assistantMessageId: String,
        timestamp: String,
    ): ClientModeStreamReducerState {
        val delta = chunk.text?.takeIf { it.isNotEmpty() } ?: return state
        return state.upsertMessage(assistantMessageId, timestamp) { existing ->
            if (existing != null) {
                existing.copy(content = existing.content + delta)
            } else {
                UiMessage(
                    id = assistantMessageId,
                    role = "assistant",
                    content = delta,
                    timestamp = timestamp,
                )
            }
        }
    }

    private fun reduceToolFrame(
        state: ClientModeStreamReducerState,
        chunk: BotStreamChunk,
        timestamp: String,
    ): ClientModeStreamReducerState {
        val incomingToolCalls = chunk.effectiveToolCalls()
        val rawToolCallId = chunk.toolFrameId(incomingToolCalls) ?: return state
        val toolCallId = if (chunk.event == BotStreamEvent.TOOL_RESULT) {
            state.toolBatchMessageIds[rawToolCallId] ?: rawToolCallId
        } else {
            rawToolCallId
        }
        val messageId = "client-tool-$toolCallId"
        val startedAtMs = if (chunk.event == BotStreamEvent.TOOL_CALL) nowMs() else null
        var nextStartedAt = state.toolStartedAtMs
        var nextBatchIds = state.toolBatchMessageIds
        if (startedAtMs != null) {
            nextStartedAt = nextStartedAt + (toolCallId to (nextStartedAt[toolCallId] ?: startedAtMs))
            incomingToolCalls.forEach { call ->
                val callId = call.effectiveId.takeIf { it.isNotBlank() } ?: return@forEach
                nextStartedAt = nextStartedAt + (callId to (nextStartedAt[callId] ?: startedAtMs))
                nextBatchIds = nextBatchIds + (callId to (nextBatchIds[callId] ?: toolCallId))
            }
        }

        val stateWithBookkeeping = state.copy(
            toolStartedAtMs = nextStartedAt,
            toolBatchMessageIds = nextBatchIds,
        )
        return stateWithBookkeeping.upsertMessage(messageId, timestamp) { existing ->
            val resultTargetId = if (chunk.event == BotStreamEvent.TOOL_RESULT) rawToolCallId else null
            val incomingUiToolCalls = incomingToolCalls.mapIndexed { index, toolCall ->
                val incomingId = toolCall.effectiveId.takeIf { it.isNotBlank() }
                val old = existing?.toolCalls.findByToolCallId(incomingId)
                    ?: existing?.toolCalls?.getOrNull(index)
                val isResultForCall = chunk.event == BotStreamEvent.TOOL_RESULT &&
                    (resultTargetId == null || incomingId == null || resultTargetId == incomingId)
                val executionTimeMs = if (isResultForCall) {
                    nextStartedAt[incomingId ?: resultTargetId ?: toolCallId]?.let { startedAt ->
                        (nowMs() - startedAt).coerceAtLeast(0L)
                    } ?: old?.executionTimeMs
                } else {
                    old?.executionTimeMs
                }
                val incomingName = toolCall.name?.takeIf {
                    it.isNotBlank() && (it != "tool" || old == null)
                }
                UiToolCall(
                    name = incomingName ?: old?.name ?: "tool",
                    arguments = toolCall.arguments?.takeIf { it.isNotBlank() } ?: old?.arguments.orEmpty(),
                    result = if (isResultForCall) chunk.text ?: old?.result else old?.result,
                    status = if (isResultForCall) {
                        if (chunk.isError) "error" else "success"
                    } else {
                        old?.status
                    },
                    executionTimeMs = executionTimeMs,
                    toolCallId = incomingId ?: old?.toolCallId,
                )
            }
            UiMessage(
                id = messageId,
                role = "assistant",
                content = "",
                timestamp = existing?.timestamp ?: timestamp,
                toolCalls = mergeStreamingUiToolCalls(
                    existing = existing?.toolCalls.orEmpty(),
                    incoming = incomingUiToolCalls,
                ),
            )
        }
    }
}

internal data class ClientModeStreamReducerState(
    val messages: List<UiMessage> = emptyList(),
    val toolStartedAtMs: Map<String, Long> = emptyMap(),
    val toolBatchMessageIds: Map<String, String> = emptyMap(),
)

private fun ClientModeStreamReducerState.upsertMessage(
    messageId: String,
    timestamp: String,
    build: (UiMessage?) -> UiMessage,
): ClientModeStreamReducerState {
    val currentMessages = messages.toMutableList()
    val index = currentMessages.indexOfFirst { it.id == messageId }
    val existing = currentMessages.getOrNull(index)
    val next = build(existing)
    if (index >= 0) {
        currentMessages[index] = next.copy(timestamp = existing?.timestamp ?: timestamp)
    } else {
        currentMessages += next.copy(timestamp = next.timestamp.ifBlank { timestamp })
    }
    return copy(messages = currentMessages)
}

private fun BotStreamChunk.effectiveToolCalls(): List<ToolCall> {
    val batchedCalls = toolCalls.orEmpty().filter { call ->
        call.id != null || call.toolCallId != null || call.name != null || call.arguments != null
    }
    if (batchedCalls.isNotEmpty()) return batchedCalls
    if (toolCallId == null && uuid == null && toolName == null && toolInput == null) return emptyList()
    return listOf(
        ToolCall(
            id = toolCallId ?: uuid,
            toolCallId = toolCallId,
            name = toolName ?: "tool",
            arguments = toolInput?.toString().orEmpty(),
        )
    )
}

private fun BotStreamChunk.toolFrameId(calls: List<ToolCall>): String? {
    return toolCallId
        ?: uuid
        ?: calls.firstOrNull()?.effectiveId?.takeIf { it.isNotBlank() }
        ?: toolName?.takeIf { it.isNotBlank() }
}

private fun List<UiToolCall>?.findByToolCallId(toolCallId: String?): UiToolCall? {
    val id = toolCallId?.takeIf { it.isNotBlank() } ?: return null
    return this?.firstOrNull { it.toolCallId == id }
}

private fun mergeStreamingUiToolCalls(
    existing: List<UiToolCall>,
    incoming: List<UiToolCall>,
): List<UiToolCall> {
    if (existing.isEmpty()) return incoming
    if (incoming.isEmpty()) return existing
    val merged = existing.toMutableList()
    incoming.forEach { incomingCall ->
        val existingIndex = incomingCall.toolCallId
            ?.takeIf { it.isNotBlank() }
            ?.let { callId -> merged.indexOfFirst { it.toolCallId == callId } }
            ?.takeIf { it >= 0 }
            ?: merged.indexOfFirst { it.name == incomingCall.name && it.toolCallId == null }
                .takeIf { it >= 0 }
        if (existingIndex == null) {
            merged += incomingCall
        } else {
            merged[existingIndex] = merged[existingIndex].mergeStreamingUpdate(incomingCall)
        }
    }
    return merged
}

private fun UiToolCall.mergeStreamingUpdate(incoming: UiToolCall): UiToolCall = copy(
    name = incoming.name.takeIf { it.isNotBlank() && (it != "tool" || name == "tool") } ?: name,
    arguments = incoming.arguments.takeIf { it.isNotBlank() } ?: arguments,
    result = incoming.result ?: result,
    status = incoming.status ?: status,
    executionTimeMs = incoming.executionTimeMs ?: executionTimeMs,
    toolCallId = incoming.toolCallId ?: toolCallId,
)
