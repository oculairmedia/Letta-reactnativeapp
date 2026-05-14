package com.letta.mobile.data.api

import com.letta.mobile.data.model.ProjectCatalog
import com.letta.mobile.data.model.ProjectDetailResponse
import com.letta.mobile.data.model.ProjectSummary
import io.ktor.client.call.body
import io.ktor.client.request.delete
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
    val status: String? = null,
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
    /**
     * letta-mobile-2ixd: capability probe for the projects API. Status code
     * alone isn't enough — some servers (e.g. the letta-code admin shim)
     * stub `/api/projects` to return `200 []` so the endpoint exists in
     * name but doesn't actually serve a project catalog. The empty array
     * can't be deserialized into [ProjectCatalog] (which requires a
     * `projects` field), so use successful deserialization as the
     * supported signal:
     *
     *   - 404 / 405 / 501 → unsupported (definitive)
     *   - 2xx with parseable [ProjectCatalog] body → supported
     *   - 2xx with un-parseable body (e.g. shim's `[]`) → unsupported
     *   - any other status / network error → assume supported, so a
     *     flaky network doesn't silently hide a working feature
     */
    open suspend fun probeAvailability(): Boolean {
        val client = apiClient.getClient()
        val baseUrl = apiClient.getBaseUrl().trimEnd('/')
        return try {
            val response = client.get("$baseUrl/api/projects?limit=1")
            when (response.status.value) {
                404, 405, 501 -> false
                in 200..299 -> runCatching { response.body<ProjectCatalog>() }.isSuccess
                else -> true
            }
        } catch (e: Exception) {
            true
        }
    }

    open suspend fun listProjects(): ProjectCatalog {
        val client = apiClient.getClient()
        val baseUrl = apiClient.getBaseUrl().trimEnd('/')

        val response = client.get("$baseUrl/api/projects")
        if (response.status.value !in 200..299) {
            throw ApiException(response.status.value, response.bodyAsText())
        }
        return response.body<ProjectCatalog>()
    }

    open suspend fun getProject(identifier: String): ProjectSummary {
        val client = apiClient.getClient()
        val baseUrl = apiClient.getBaseUrl().trimEnd('/')

        val response = client.get("$baseUrl/api/projects/$identifier")
        if (response.status.value !in 200..299) {
            throw ApiException(response.status.value, response.bodyAsText())
        }
        return response.body<ProjectDetailResponse>().project
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

    open suspend fun archiveProject(identifier: String): ProjectSummary {
        return updateProject(
            identifier = identifier,
            request = ProjectUpdateRequest(status = "archived"),
        )
    }

    open suspend fun deleteProject(identifier: String) {
        val client = apiClient.getClient()
        val baseUrl = apiClient.getBaseUrl().trimEnd('/')

        val response = client.delete("$baseUrl/api/registry/projects/$identifier")
        if (response.status.value !in 200..299) {
            throw ApiException(response.status.value, response.bodyAsText())
        }
    }
}
