package com.letta.mobile.data.api

import com.letta.mobile.data.model.ProjectCatalog
import com.letta.mobile.data.model.ProjectSummary
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class ProjectApi @Inject constructor(
    private val apiClient: LettaApiClient,
) {
    open suspend fun listProjects(): ProjectCatalog {
        val client = apiClient.getClient()
        val baseUrl = apiClient.getBaseUrl().trimEnd('/')

        val response = client.get("$baseUrl/api/registry/projects")
        if (response.status.value !in 200..299) {
            throw ApiException(response.status.value, response.bodyAsText())
        }
        return response.body()
    }

    open suspend fun getProject(identifier: String): ProjectSummary {
        val client = apiClient.getClient()
        val baseUrl = apiClient.getBaseUrl().trimEnd('/')

        val response = client.get("$baseUrl/api/registry/projects/$identifier")
        if (response.status.value !in 200..299) {
            throw ApiException(response.status.value, response.bodyAsText())
        }
        return response.body()
    }
}
