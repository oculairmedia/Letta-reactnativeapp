package com.letta.mobile.data.api

import com.letta.mobile.data.model.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class ConversationApi @Inject constructor(
    private val apiClient: LettaApiClient
) {
    open suspend fun listConversations(
        agentId: AgentId? = null,
        limit: Int? = null,
        after: String? = null,
        archiveStatus: String? = null,
        summarySearch: String? = null,
        order: String? = null,
        orderBy: String? = null,
    ): List<Conversation> {
        val (client, baseUrl) = apiClient.session()

        val response = client.get("$baseUrl/v1/conversations") {
            parameter("agent_id", agentId?.value)
            parameter("limit", limit)
            parameter("after", after)
            parameter("archive_status", archiveStatus)
            parameter("summary_search", summarySearch)
            parameter("order", order)
            parameter("order_by", orderBy)
        }
        if (response.status.value !in 200..299) {
            throw ApiException(response.status.value, response.bodyAsText())
        }
        return response.body()
    }

    open suspend fun getConversation(conversationId: ConversationId): Conversation {
        val (client, baseUrl) = apiClient.session()

        val response = client.get("$baseUrl/v1/conversations/${conversationId.value}")
        if (response.status.value !in 200..299) {
            throw ApiException(response.status.value, response.bodyAsText())
        }
        return response.body()
    }

    open suspend fun createConversation(params: ConversationCreateParams): Conversation {
        val (client, baseUrl) = apiClient.session()

        val response = client.post("$baseUrl/v1/conversations") {
            contentType(ContentType.Application.Json)
            parameter("agent_id", params.agentId.value)
            setBody(params)
        }
        if (response.status.value !in 200..299) {
            throw ApiException(response.status.value, response.bodyAsText())
        }
        return response.body()
    }

    open suspend fun updateConversation(conversationId: ConversationId, params: ConversationUpdateParams): Conversation {
        val (client, baseUrl) = apiClient.session()

        val response = client.patch("$baseUrl/v1/conversations/${conversationId.value}") {
            contentType(ContentType.Application.Json)
            setBody(params)
        }
        if (response.status.value !in 200..299) {
            throw ApiException(response.status.value, response.bodyAsText())
        }
        return response.body()
    }

    open suspend fun deleteConversation(conversationId: ConversationId) {
        val (client, baseUrl) = apiClient.session()

        val response = client.delete("$baseUrl/v1/conversations/${conversationId.value}")
        if (response.status.value !in 200..299) {
            throw ApiException(response.status.value, response.bodyAsText())
        }
    }

    open suspend fun forkConversation(conversationId: ConversationId, agentId: AgentId? = null): Conversation {
        val (client, baseUrl) = apiClient.session()

        val response = client.post("$baseUrl/v1/conversations/${conversationId.value}/fork") {
            contentType(ContentType.Application.Json)
            agentId?.let { parameter("agent_id", it.value) }
        }
        if (response.status.value !in 200..299) {
            throw ApiException(response.status.value, response.bodyAsText())
        }
        return response.body()
    }

    open suspend fun cancelConversation(conversationId: ConversationId, agentId: AgentId? = null) {
        val (client, baseUrl) = apiClient.session()

        val response = client.post("$baseUrl/v1/conversations/${conversationId.value}/cancel") {
            contentType(ContentType.Application.Json)
            agentId?.let { parameter("agent_id", it.value) }
        }
        if (response.status.value !in 200..299) {
            throw ApiException(response.status.value, response.bodyAsText())
        }
    }

    open suspend fun recompileConversation(
        conversationId: ConversationId,
        dryRun: Boolean = false,
        agentId: AgentId? = null,
    ): String {
        val (client, baseUrl) = apiClient.session()

        val response = client.post("$baseUrl/v1/conversations/${conversationId.value}/recompile") {
            contentType(ContentType.Application.Json)
            parameter("dry_run", dryRun)
            setBody(
                buildMap<String, String?> {
                    put("agent_id", agentId?.value)
                }.filterValues { it != null }
            )
        }
        if (response.status.value !in 200..299) {
            throw ApiException(response.status.value, response.bodyAsText())
        }
        return response.body()
    }
}
