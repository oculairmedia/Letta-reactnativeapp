package com.letta.mobile.data.api

import com.letta.mobile.data.model.ScheduleCreateParams
import com.letta.mobile.data.model.ScheduleListResponse
import com.letta.mobile.data.model.ScheduledMessage
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleApi @Inject constructor(
    private val apiClient: LettaApiClient,
) {
    suspend fun listSchedules(
        agentId: String,
        limit: Int? = null,
        after: String? = null,
    ): ScheduleListResponse {
        val client = apiClient.getClient()
        val baseUrl = apiClient.getBaseUrl()

        val response = client.get("$baseUrl/v1/agents/$agentId/schedule") {
            parameter("limit", limit?.toString())
            parameter("after", after)
        }
        if (response.status.value !in 200..299) {
            throw ApiException(response.status.value, response.bodyAsText())
        }
        return response.body()
    }

    suspend fun retrieveSchedule(agentId: String, scheduledMessageId: String): ScheduledMessage {
        val client = apiClient.getClient()
        val baseUrl = apiClient.getBaseUrl()

        val response = client.get("$baseUrl/v1/agents/$agentId/schedule/$scheduledMessageId")
        if (response.status.value !in 200..299) {
            throw ApiException(response.status.value, response.bodyAsText())
        }
        return response.body()
    }

    suspend fun createSchedule(agentId: String, params: ScheduleCreateParams): ScheduledMessage {
        val client = apiClient.getClient()
        val baseUrl = apiClient.getBaseUrl()

        val response = client.post("$baseUrl/v1/agents/$agentId/schedule") {
            contentType(ContentType.Application.Json)
            setBody(params)
        }
        if (response.status.value !in 200..299) {
            throw ApiException(response.status.value, response.bodyAsText())
        }
        return response.body()
    }

    suspend fun deleteSchedule(agentId: String, scheduledMessageId: String) {
        val client = apiClient.getClient()
        val baseUrl = apiClient.getBaseUrl()

        val response = client.delete("$baseUrl/v1/agents/$agentId/schedule/$scheduledMessageId")
        if (response.status.value !in 200..299) {
            throw ApiException(response.status.value, response.bodyAsText())
        }
    }
}
