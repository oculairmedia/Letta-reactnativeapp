package com.letta.mobile.chat.model

sealed interface StreamEvent {
    data object Connecting : StreamEvent
    data object Sending : StreamEvent
    data class Streaming(val messages: List<ChatMessage>) : StreamEvent
    data class ToolExecution(val toolName: String) : StreamEvent
    data class Complete(val messages: List<ChatMessage>) : StreamEvent
    data class Error(val message: String) : StreamEvent
}
