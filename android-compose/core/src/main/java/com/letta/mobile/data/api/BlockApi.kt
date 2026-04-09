package com.letta.mobile.data.api

import com.letta.mobile.data.model.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class BlockApi @Inject constructor(
    private val apiClient: LettaApiClient
) {
    open suspend fun getBlock(agentId: String, blockLabel: String): Block {
        val client = apiClient.getClient()
        val baseUrl = apiClient.getBaseUrl()

        val response = client.get("$baseUrl/v1/agents/$agentId/blocks/$blockLabel")
        if (response.status.value !in 200..299) {
            throw ApiException(response.status.value, response.bodyAsText())
        }
        return response.body()
    }

    open suspend fun updateBlock(agentId: String, blockLabel: String, params: BlockUpdateParams): Block {
        val client = apiClient.getClient()
        val baseUrl = apiClient.getBaseUrl()

        val response = client.patch("$baseUrl/v1/agents/$agentId/blocks/$blockLabel") {
            contentType(ContentType.Application.Json)
            setBody(params)
        }
        if (response.status.value !in 200..299) {
            throw ApiException(response.status.value, response.bodyAsText())
        }
        return response.body()
    }

    open suspend fun createBlock(params: BlockCreateParams): Block {
        val client = apiClient.getClient()
        val baseUrl = apiClient.getBaseUrl()

        val response = client.post("$baseUrl/v1/blocks") {
            contentType(ContentType.Application.Json)
            setBody(params)
        }
        if (response.status.value !in 200..299) {
            throw ApiException(response.status.value, response.bodyAsText())
        }
        return response.body()
    }

    open suspend fun deleteBlock(blockId: String) {
        val client = apiClient.getClient()
        val baseUrl = apiClient.getBaseUrl()

        val response = client.delete("$baseUrl/v1/blocks/$blockId")
        if (response.status.value !in 200..299) {
            throw ApiException(response.status.value, response.bodyAsText())
        }
    }

    open suspend fun attachBlock(agentId: String, blockId: String): Block {
        val client = apiClient.getClient()
        val baseUrl = apiClient.getBaseUrl()

        val response = client.post("$baseUrl/v1/agents/$agentId/blocks/$blockId/attach")
        if (response.status.value !in 200..299) {
            throw ApiException(response.status.value, response.bodyAsText())
        }
        return response.body()
    }

    open suspend fun detachBlock(agentId: String, blockId: String) {
        val client = apiClient.getClient()
        val baseUrl = apiClient.getBaseUrl()

        val response = client.post("$baseUrl/v1/agents/$agentId/blocks/$blockId/detach")
        if (response.status.value !in 200..299) {
            throw ApiException(response.status.value, response.bodyAsText())
        }
    }

    open suspend fun listBlocks(agentId: String): List<Block> {
        val client = apiClient.getClient()
        val baseUrl = apiClient.getBaseUrl()

        val response = client.get("$baseUrl/v1/agents/$agentId/blocks")
        if (response.status.value !in 200..299) {
            throw ApiException(response.status.value, response.bodyAsText())
        }
        return response.body()
    }

    open suspend fun listAllBlocks(
        label: String? = null,
        isTemplate: Boolean? = null,
        limit: Int? = null,
        offset: Int? = null,
    ): List<Block> {
        val client = apiClient.getClient()
        val baseUrl = apiClient.getBaseUrl()

        val response = client.get("$baseUrl/v1/blocks") {
            parameter("label", label)
            parameter("is_template", isTemplate)
            parameter("limit", limit)
            parameter("offset", offset)
        }
        if (response.status.value !in 200..299) {
            throw ApiException(response.status.value, response.bodyAsText())
        }
        return response.body()
    }
}
