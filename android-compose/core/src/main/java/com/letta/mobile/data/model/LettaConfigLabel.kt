package com.letta.mobile.data.model

fun LettaConfig?.toBackendLabel(): String? {
    val config = this ?: return null
    return when (config.mode) {
        LettaConfig.Mode.CLOUD -> "Cloud"
        LettaConfig.Mode.SELF_HOSTED -> config.serverUrl
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore('/')
            .ifBlank { "Server" }
    }
}
