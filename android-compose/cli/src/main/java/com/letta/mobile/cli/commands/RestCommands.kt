package com.letta.mobile.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.letta.mobile.cli.runtime.CliJson
import com.letta.mobile.cli.runtime.buildRestUrl
import com.letta.mobile.cli.runtime.cliHttpClient
import com.letta.mobile.cli.runtime.parseQueryParams
import com.letta.mobile.cli.runtime.resolveRequestBody
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

internal class RestCommand : CliktCommand(
    name = "rest",
) {
    override fun run() = Unit
}

internal abstract class RestVerbCommand(
    name: String,
    private val verb: String,
) : AdminShimCommand(
    name = name,
    help = "$verb a Letta REST endpoint.",
) {
    private val path by argument("path")
    private val query by option("--query", "-q", help = "Query parameter as name=value. Repeatable.").multiple()
    private val body by option("--body", help = "Raw JSON request body.")
    private val bodyFile by option("--body-file", help = "Path to a JSON request body file.")
    private val compact by option("--compact", help = "Print compact JSON response.").flag(default = false)
    private val raw by option("--raw", help = "Print response body without JSON formatting.").flag(default = false)
    private val allowError by option("--allow-error", help = "Do not fail the process on non-2xx HTTP responses.").flag(default = false)

    override fun run() = runBlocking {
        val requestBody = resolveRequestBody(body, bodyFile)
        val url = buildRestUrl(baseUrl, path, parseQueryParams(query))
        val client = cliHttpClient()
        try {
            val response = client.executeRestRequest(verb, url, token, requestBody)
            val text = response.bodyAsText()
            if (response.status.value !in 200..299) {
                System.err.println("[rest] HTTP ${response.status.value} ${response.status.description}")
                if (text.isNotBlank()) System.err.println(text)
                if (!allowError) {
                    throw IllegalStateException("REST request failed: HTTP ${response.status.value}")
                }
            }
            printResponse(text, compact, raw)
        } finally {
            client.close()
        }
    }
}

internal class RestGetCommand : RestVerbCommand("get", "GET")
internal class RestPostCommand : RestVerbCommand("post", "POST")
internal class RestPatchCommand : RestVerbCommand("patch", "PATCH")
internal class RestDeleteCommand : RestVerbCommand("delete", "DELETE")

private suspend fun HttpClient.executeRestRequest(
    verb: String,
    url: String,
    token: String,
    body: String?,
): HttpResponse = when (verb) {
    "GET" -> get(url) {
        bearerAuth(token)
        header(HttpHeaders.Accept, ContentType.Application.Json)
    }
    "POST" -> post(url) {
        bearerAuth(token)
        header(HttpHeaders.Accept, ContentType.Application.Json)
        body?.let {
            contentType(ContentType.Application.Json)
            setBody(it)
        }
    }
    "PATCH" -> patch(url) {
        bearerAuth(token)
        header(HttpHeaders.Accept, ContentType.Application.Json)
        body?.let {
            contentType(ContentType.Application.Json)
            setBody(it)
        }
    }
    "DELETE" -> delete(url) {
        bearerAuth(token)
        header(HttpHeaders.Accept, ContentType.Application.Json)
        body?.let {
            contentType(ContentType.Application.Json)
            setBody(it)
        }
    }
    else -> error("Unsupported REST verb: $verb")
}

private fun printResponse(text: String, compact: Boolean, raw: Boolean) {
    if (text.isBlank()) return
    if (raw) {
        println(text)
        return
    }
    val parsed = runCatching { CliJson.parseToJsonElement(text) }.getOrNull()
    if (parsed == null) {
        println(text)
        return
    }
    val encoder = if (compact) compactJson else prettyJson
    println(encoder.encodeToString(JsonElement.serializer(), parsed))
}

private val prettyJson = Json {
    prettyPrint = true
    explicitNulls = false
    encodeDefaults = true
}

private val compactJson = Json {
    explicitNulls = false
    encodeDefaults = true
}
