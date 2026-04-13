package com.letta.mobile.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class UiMessage(
    val id: String,
    val role: String,
    val content: String,
    val timestamp: String,
    val isReasoning: Boolean = false,
    val toolCalls: List<UiToolCall>? = null,
    val generatedUi: UiGeneratedComponent? = null,
)

@Immutable
data class UiToolCall(
    val name: String,
    val arguments: String,
    val result: String?,
    val status: String? = null,
)

@Immutable
data class UiGeneratedComponent(
    val name: String,
    val propsJson: String,
    val fallbackText: String? = null,
)
