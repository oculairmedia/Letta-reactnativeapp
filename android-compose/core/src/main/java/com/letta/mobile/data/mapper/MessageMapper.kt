package com.letta.mobile.data.mapper

import com.letta.mobile.data.model.AppMessage
import com.letta.mobile.data.model.ApprovalDecisionPayload
import com.letta.mobile.data.model.ApprovalRequestMessage
import com.letta.mobile.data.model.ApprovalRequestPayload
import com.letta.mobile.data.model.ApprovalResponseMessage
import com.letta.mobile.data.model.ApprovalResponsePayload
import com.letta.mobile.data.model.ApprovalToolCallPayload
import com.letta.mobile.data.model.AssistantMessage
import com.letta.mobile.data.model.GeneratedUiPayload
import com.letta.mobile.data.model.LettaMessage
import com.letta.mobile.data.model.MessageType
import com.letta.mobile.data.model.ReasoningMessage
import com.letta.mobile.data.model.ToolCallMessage
import com.letta.mobile.data.model.ToolReturnMessage
import com.letta.mobile.data.model.UserMessage
import com.letta.mobile.data.model.UiApprovalDecision
import com.letta.mobile.data.model.UiApprovalRequest
import com.letta.mobile.data.model.UiApprovalResponse
import com.letta.mobile.data.model.UiApprovalToolCall
import com.letta.mobile.data.model.UiGeneratedComponent
import com.letta.mobile.data.model.UiMessage
import com.letta.mobile.data.model.UiToolCall
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

internal data class ToolCallContext(
    val name: String,
    val arguments: String,
)

class MessageMappingState internal constructor(
    internal val toolCallsById: MutableMap<String, ToolCallContext> = mutableMapOf(),
)

private val generatedUiToolNames = setOf(
    "render_summary_card",
    "render_metric_card",
    "render_suggestion_chips",
)

fun List<LettaMessage>.toAppMessages(): List<AppMessage> {
    val state = MessageMappingState()
    return mapNotNull { it.toAppMessage(state) }
}

fun LettaMessage.toAppMessage(): AppMessage? {
    return toAppMessage(MessageMappingState())
}

fun LettaMessage.toAppMessage(state: MessageMappingState): AppMessage? {
    return when (this) {
        is UserMessage -> AppMessage(
            id = id,
            date = date?.toInstant() ?: Instant.now(),
            messageType = MessageType.USER,
            content = content
        )
        is AssistantMessage -> {
            val generatedUi = extractGeneratedUi(contentRaw)
            AppMessage(
                id = id,
                date = date?.toInstant() ?: Instant.now(),
                messageType = MessageType.ASSISTANT,
                content = generatedUi?.fallbackText.orEmpty().ifBlank {
                    if (generatedUi != null) "" else content
                },
                generatedUi = generatedUi,
            )
        }
        is ReasoningMessage -> AppMessage(
            id = id,
            date = date?.toInstant() ?: Instant.now(),
            messageType = MessageType.REASONING,
            content = reasoning
        )
        is ToolCallMessage -> {
            val toolCall = effectiveToolCalls.firstOrNull()
            val toolCallId = toolCall?.effectiveId
            val toolName = toolCall?.name
            val arguments = toolCall?.arguments.orEmpty()
            if (!toolCallId.isNullOrBlank() && !toolName.isNullOrBlank()) {
                state.toolCallsById[toolCallId] = ToolCallContext(name = toolName, arguments = arguments)
            }
            AppMessage(
                id = id,
                date = date?.toInstant() ?: Instant.now(),
                messageType = MessageType.TOOL_CALL,
                content = arguments,
                toolName = toolName,
                toolCallId = toolCallId,
            )
        }
        is ApprovalRequestMessage -> {
            val toolCalls = effectiveToolCalls.mapNotNull { toolCall ->
                val toolCallId = toolCall.effectiveId.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val toolName = toolCall.name?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val arguments = toolCall.arguments.orEmpty()
                state.toolCallsById[toolCallId] = ToolCallContext(name = toolName, arguments = arguments)
                ApprovalToolCallPayload(
                    toolCallId = toolCallId,
                    name = toolName,
                    arguments = arguments,
                )
            }
            AppMessage(
                id = id,
                date = date?.toInstant() ?: Instant.now(),
                messageType = MessageType.APPROVAL_REQUEST,
                content = "",
                approvalRequest = ApprovalRequestPayload(
                    requestId = id,
                    toolCalls = toolCalls,
                ),
            )
        }
        is ToolReturnMessage -> {
            val toolCallId = toolReturn.toolCallId
            val context = state.toolCallsById[toolCallId]
            AppMessage(
                id = id,
                date = date?.toInstant() ?: Instant.now(),
                messageType = MessageType.TOOL_RETURN,
                content = toolReturn.funcResponse ?: "",
                toolName = context?.name ?: name,
                toolCallId = toolCallId,
                toolReturnStatus = toolReturn.status,
            )
        }
        is ApprovalResponseMessage -> AppMessage(
            id = id,
            date = date?.toInstant() ?: Instant.now(),
            messageType = MessageType.APPROVAL_RESPONSE,
            content = "",
            approvalResponse = ApprovalResponsePayload(
                requestId = approvalRequestId,
                approved = approve,
                reason = reason,
                approvals = approvals.orEmpty().mapNotNull { approval ->
                    val toolCallId = approval.toolCallId?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    ApprovalDecisionPayload(
                        toolCallId = toolCallId,
                        approved = approval.approve,
                        status = approval.status,
                        reason = approval.reason,
                    )
                },
            ),
        )
        else -> null
    }
}

fun List<AppMessage>.toUiMessages(): List<UiMessage> {
    val returnsByCallId = mutableMapOf<String, AppMessage>()
    for (msg in this) {
        if (msg.messageType == MessageType.TOOL_RETURN && !msg.toolCallId.isNullOrBlank()) {
            returnsByCallId[msg.toolCallId] = msg
        }
    }

    val consumedReturnIds = mutableSetOf<String>()

    val result = mutableListOf<UiMessage>()
    for (msg in this) {
        when (msg.messageType) {
            MessageType.TOOL_CALL -> {
                val matchedReturn = msg.toolCallId?.let(returnsByCallId::get)
                if (matchedReturn != null) {
                    consumedReturnIds.add(matchedReturn.id)
                }
                val name = msg.toolName
                val arguments = msg.content
                val returnContent = matchedReturn?.content
                val returnStatus = matchedReturn?.toolReturnStatus

                if (name in generatedUiToolNames && returnContent != null) {
                    val generatedUi = extractGeneratedUiFromString(returnContent)
                    if (generatedUi != null) {
                        result.add(
                            UiMessage(
                                id = msg.id,
                                role = "assistant",
                                content = generatedUi.fallbackText.orEmpty(),
                                timestamp = msg.date.toString(),
                                generatedUi = UiGeneratedComponent(
                                    name = generatedUi.component,
                                    propsJson = generatedUi.propsJson,
                                    fallbackText = generatedUi.fallbackText,
                                ),
                            )
                        )
                        continue
                    }
                }

                // send_message is Letta's reply tool — promote to assistant bubble
                if (name == "send_message" && returnContent != null) {
                    val visibleText = extractSendMessageText(arguments, returnContent)
                    if (visibleText.isNotBlank()) {
                        result.add(UiMessage(
                            id = msg.id,
                            role = "assistant",
                            content = visibleText,
                            timestamp = msg.date.toString(),
                        ))
                        continue
                    }
                }

                val toolCall = UiToolCall(
                    name = name ?: "tool",
                    arguments = arguments,
                    result = returnContent,
                    status = returnStatus,
                )
                result.add(UiMessage(
                    id = msg.id,
                    role = "tool",
                    content = "",
                    timestamp = msg.date.toString(),
                    toolCalls = listOf(toolCall),
                ))
            }

            MessageType.TOOL_RETURN -> {
                if (msg.id in consumedReturnIds) continue
                val name = msg.toolName ?: "tool"

                if (name in generatedUiToolNames && msg.content.isNotBlank()) {
                    val generatedUi = extractGeneratedUiFromString(msg.content)
                    if (generatedUi != null) {
                        result.add(
                            UiMessage(
                                id = msg.id,
                                role = "assistant",
                                content = generatedUi.fallbackText.orEmpty(),
                                timestamp = msg.date.toString(),
                                generatedUi = UiGeneratedComponent(
                                    name = generatedUi.component,
                                    propsJson = generatedUi.propsJson,
                                    fallbackText = generatedUi.fallbackText,
                                ),
                            )
                        )
                        continue
                    }
                }

                if (name == "send_message" && msg.content.isNotBlank()) {
                    result.add(UiMessage(
                        id = msg.id,
                        role = "assistant",
                        content = msg.content,
                        timestamp = msg.date.toString(),
                    ))
                    continue
                }

                val toolCall = UiToolCall(
                    name = name,
                    arguments = "",
                    result = msg.content.ifBlank { null },
                    status = msg.toolReturnStatus,
                )
                result.add(UiMessage(
                    id = msg.id,
                    role = "tool",
                    content = "",
                    timestamp = msg.date.toString(),
                    toolCalls = listOf(toolCall),
                ))
            }

            MessageType.USER -> result.add(msg.toUiMessage())
            MessageType.ASSISTANT -> result.add(msg.toUiMessage())
            MessageType.REASONING -> result.add(msg.toUiMessage())
            MessageType.APPROVAL_REQUEST -> result.add(msg.toUiMessage())
            MessageType.APPROVAL_RESPONSE -> {
                // Suppress auto-approval response cards: in bypassPermissions sessions
                // the Letta server emits approval_response_message entries with
                // approve=null (and per-call approvals[].approve=null). These are
                // NOT user-facing rejections — they are bookkeeping echoes of the
                // tool_return. Rendering them as "Rejected" cards (the legacy
                // behaviour) misled operators on every yolo session.
                //
                // Only render the card when SOMEONE actually made an explicit
                // decision (approve == true OR approve == false at either the
                // top-level or any per-call entry).
                val response = msg.approvalResponse
                val hasExplicitDecision = response != null && (
                    response.approved != null ||
                    response.approvals.any { it.approved != null }
                )
                if (hasExplicitDecision) {
                    result.add(msg.toUiMessage())
                }
                // else: silently drop — the linked tool_return already carries
                // the actual outcome and is rendered by the TOOL_CALL/TOOL_RETURN
                // branches above.
            }
        }
    }
    return result
}

private fun extractSendMessageText(arguments: String, returnContent: String): String {
    return try {
        val msgStart = arguments.indexOf("\"message\"")
        if (msgStart < 0) return returnContent
        val colonIdx = arguments.indexOf(':', msgStart)
        if (colonIdx < 0) return returnContent
        val valStart = arguments.indexOf('"', colonIdx + 1)
        if (valStart < 0) return returnContent
        var i = valStart + 1
        val sb = StringBuilder()
        while (i < arguments.length) {
            val c = arguments[i]
            if (c == '\\' && i + 1 < arguments.length) {
                val next = arguments[i + 1]
                when (next) {
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    'n' -> sb.append('\n')
                    't' -> sb.append('\t')
                    else -> { sb.append('\\'); sb.append(next) }
                }
                i += 2
            } else if (c == '"') {
                break
            } else {
                sb.append(c)
                i++
            }
        }
        sb.toString().ifBlank { returnContent }
    } catch (_: Exception) {
        returnContent
    }
}

fun AppMessage.toUiMessage(): UiMessage {
    val role = when (messageType) {
        MessageType.USER -> "user"
        MessageType.ASSISTANT -> "assistant"
        MessageType.REASONING -> "assistant"
        MessageType.TOOL_CALL -> "tool"
        MessageType.TOOL_RETURN -> "tool"
        MessageType.APPROVAL_REQUEST -> "approval"
        MessageType.APPROVAL_RESPONSE -> "approval"
    }
    val toolCalls = when {
        messageType == MessageType.TOOL_CALL -> {
            listOf(UiToolCall(name = toolName ?: "tool", arguments = content, result = null))
        }
        messageType == MessageType.TOOL_RETURN -> {
            listOf(UiToolCall(name = toolName ?: "tool", arguments = "", result = content.ifBlank { null }))
        }
        else -> null
    }
    val displayContent = when {
        messageType == MessageType.TOOL_CALL -> ""
        messageType == MessageType.TOOL_RETURN -> ""
        else -> content
    }

    return UiMessage(
        id = id,
        role = role,
        content = displayContent,
        timestamp = date.toString(),
        isPending = isPending,
        isReasoning = messageType == MessageType.REASONING,
        toolCalls = toolCalls,
        generatedUi = generatedUi?.let {
            UiGeneratedComponent(
                name = it.component,
                propsJson = it.propsJson,
                fallbackText = it.fallbackText,
            )
        },
        approvalRequest = approvalRequest?.let {
            UiApprovalRequest(
                requestId = it.requestId,
                toolCalls = it.toolCalls.map { toolCall ->
                    UiApprovalToolCall(
                        toolCallId = toolCall.toolCallId,
                        name = toolCall.name,
                        arguments = toolCall.arguments,
                    )
                },
            )
        },
        approvalResponse = approvalResponse?.let {
            UiApprovalResponse(
                requestId = it.requestId,
                approved = it.approved,
                reason = it.reason,
                approvals = it.approvals.map { approval ->
                    UiApprovalDecision(
                        toolCallId = approval.toolCallId,
                        approved = approval.approved,
                        status = approval.status,
                        reason = approval.reason,
                    )
                },
            )
        },
    )
}

private fun extractGeneratedUi(raw: kotlinx.serialization.json.JsonElement?): GeneratedUiPayload? {
    val obj = raw as? JsonObject ?: return null
    val type = obj["type"]?.jsonPrimitive?.contentOrNull ?: return null
    if (type != "generated_ui") return null

    val component = obj["component"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: return null
    val props = obj["props"]
    val propsJson = when (props) {
        null -> buildJsonObject {}.toString()
        else -> props.toString()
    }
    val fallbackText = obj["text"]?.jsonPrimitive?.contentOrNull
        ?: obj["fallback_text"]?.jsonPrimitive?.contentOrNull

    return GeneratedUiPayload(
        component = component,
        propsJson = propsJson,
        fallbackText = fallbackText,
    )
}

private fun extractGeneratedUiFromString(raw: String): GeneratedUiPayload? {
    if (raw.isBlank()) return null
    return runCatching {
        extractGeneratedUi(Json.parseToJsonElement(raw))
    }.getOrNull()
}

private fun String.toInstant(): Instant {
    return try {
        Instant.parse(this)
    } catch (e: Exception) {
        Instant.now()
    }
}
