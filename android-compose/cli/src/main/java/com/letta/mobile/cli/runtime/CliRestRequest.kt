package com.letta.mobile.cli.runtime

import com.github.ajalt.clikt.core.UsageError
import java.nio.file.Files
import java.nio.file.Path

internal data class CliQueryParam(
    val name: String,
    val value: String,
)

internal fun buildRestUrl(
    baseUrl: String,
    path: String,
    queryParams: List<CliQueryParam>,
): String {
    val normalizedBase = baseUrl.trimEnd('/')
    val normalizedPath = path.trimStart('/')
    val baseWithPath = if (normalizedPath.isBlank()) normalizedBase else "$normalizedBase/$normalizedPath"
    if (queryParams.isEmpty()) return baseWithPath
    val query = queryParams.joinToString("&") { param ->
        "${param.name.urlEncode()}=${param.value.urlEncode()}"
    }
    return "$baseWithPath?$query"
}

internal fun parseQueryParams(rawParams: List<String>): List<CliQueryParam> =
    rawParams.map { raw ->
        val index = raw.indexOf('=')
        if (index <= 0) {
            throw UsageError("--query values must be name=value")
        }
        val name = raw.substring(0, index).trim()
        val value = raw.substring(index + 1)
        if (name.isBlank()) {
            throw UsageError("--query values must include a non-empty name")
        }
        CliQueryParam(name, value)
    }

internal fun resolveRequestBody(
    inlineBody: String?,
    bodyFile: String?,
): String? {
    if (inlineBody != null && bodyFile != null) {
        throw UsageError("Use only one of --body or --body-file")
    }
    return when {
        inlineBody != null -> inlineBody
        bodyFile != null -> String(Files.readAllBytes(Path.of(bodyFile)), Charsets.UTF_8)
        else -> null
    }
}

private fun String.urlEncode(): String =
    java.net.URLEncoder.encode(this, Charsets.UTF_8).replace("+", "%20")
