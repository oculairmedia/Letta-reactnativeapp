package com.letta.mobile.data.api

import com.letta.mobile.data.model.ProjectCatalog
import com.letta.mobile.data.model.ProjectSummary
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProjectCreateRequest(
    val name: String? = null,
    @SerialName("filesystem_path") val filesystemPath: String,
    @SerialName("git_url") val gitUrl: String? = null,
)

@Serializable
data class ProjectUpdateRequest(
    @SerialName("filesystem_path") val filesystemPath: String? = null,
    @SerialName("git_url") val gitUrl: String? = null,
)

@Serializable
private data class ProjectMutationResponse(
    val project: ProjectSummary,
)

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

    open suspend fun createProject(request: ProjectCreateRequest): ProjectSummary {
        val client = apiClient.getClient()
        val baseUrl = apiClient.getBaseUrl().trimEnd('/')

        val response = client.post("$baseUrl/api/registry/projects") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status.value !in 200..299) {
            throw ApiException(response.status.value, response.bodyAsText())
        }
        return response.body<ProjectMutationResponse>().project
    }

    open suspend fun updateProject(identifier: String, request: ProjectUpdateRequest): ProjectSummary {
        val client = apiClient.getClient()
        val baseUrl = apiClient.getBaseUrl().trimEnd('/')

        val response = client.patch("$baseUrl/api/registry/projects/$identifier") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status.value !in 200..299) {
            throw ApiException(response.status.value, response.bodyAsText())
        }
        return response.body<ProjectMutationResponse>().project
    }
}
