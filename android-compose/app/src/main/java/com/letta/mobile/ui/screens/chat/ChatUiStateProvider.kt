package com.letta.mobile.ui.screens.chat

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.letta.mobile.ui.common.UiState

val sampleMessages = listOf(
    Message(id = "1", role = "user", content = "Hello! Can you help me?", timestamp = "2024-03-15T10:00:00Z"),
    Message(id = "2", role = "assistant", content = "Of course! I'd be happy to help. What would you like to know?", timestamp = "2024-03-15T10:00:05Z"),
    Message(id = "3", role = "user", content = "What is Kotlin?", timestamp = "2024-03-15T10:01:00Z"),
    Message(
        id = "4", role = "assistant",
        content = "**Kotlin** is a modern programming language that targets the JVM, Android, JavaScript, and Native platforms.\n\nKey features:\n- Null safety\n- Extension functions\n- Coroutines for async code\n- Data classes",
        timestamp = "2024-03-15T10:01:10Z"
    ),
    Message(id = "5", role = "assistant", content = "Let me look that up for you.", timestamp = "2024-03-15T10:02:00Z", isReasoning = true),
    Message(
        id = "6", role = "tool", content = "", timestamp = "2024-03-15T10:02:05Z",
        toolCalls = listOf(ToolCall(name = "web_search", arguments = "{\"query\": \"Kotlin\"}", result = "Found 10 results"))
    ),
)

class ChatUiStateProvider : PreviewParameterProvider<UiState<ChatUiState>> {
    override val values = sequenceOf(
        UiState.Loading,
        UiState.Success(ChatUiState()),
        UiState.Success(ChatUiState(messages = sampleMessages, agentName = "Letta Agent")),
        UiState.Success(ChatUiState(messages = sampleMessages, isStreaming = true, isAgentTyping = true, agentName = "Letta Agent")),
        UiState.Error("Failed to connect to server"),
    )
}
