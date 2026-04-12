package com.letta.mobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Serializable
data class CancelAgentRunRequest(
    @SerialName("run_ids") val runIds: List<String>? = null,
)

@Serializable
data class MessageSearchRequest(
    val query: String? = null,
    @SerialName("search_mode") val searchMode: String = "hybrid",
    val roles: List<String>? = null,
    @SerialName("project_id") val projectId: String? = null,
    @SerialName("template_id") val templateId: String? = null,
    val limit: Int = 50,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
)

@Serializable
data class MessageSearchResult(
    @SerialName("embedded_text") val embeddedText: String,
    val message: JsonObject,
)

data class ParsedSearchMessage(
    val messageId: String?,
    val agentId: String?,
    val role: String?,
    val content: String?,
    val date: String?,
    val conversationId: String?,
)

fun MessageSearchResult.toParsed(): ParsedSearchMessage {
    return ParsedSearchMessage(
        messageId = (message["id"] as? JsonPrimitive)?.contentOrNull,
        agentId = (message["agent_id"] as? JsonPrimitive)?.contentOrNull,
        role = (message["role"] as? JsonPrimitive)?.contentOrNull,
        content = (message["content"] as? JsonPrimitive)?.contentOrNull ?: embeddedText,
        date = (message["date"] as? JsonPrimitive)?.contentOrNull,
        conversationId = (message["conversation_id"] as? JsonPrimitive)?.contentOrNull,
    )
}
