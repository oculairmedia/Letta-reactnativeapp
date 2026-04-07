package com.letta.mobile.ui.screens.agentlist

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.letta.mobile.data.model.Agent
import com.letta.mobile.ui.common.UiState

val sampleAgents = listOf(
    Agent(id = "1", name = "General Assistant", model = "letta/letta-free", description = "A general-purpose agent", tags = listOf("default", "chat")),
    Agent(id = "2", name = "Code Helper", model = "openai/gpt-4o", description = "Specialized in programming", tags = listOf("code")),
    Agent(id = "3", name = "Research Bot", model = "anthropic/claude-3.5-sonnet", tags = listOf("research", "analysis")),
)

class AgentListUiStateProvider : PreviewParameterProvider<UiState<AgentListUiState>> {
    override val values = sequenceOf(
        UiState.Loading,
        UiState.Success(AgentListUiState()),
        UiState.Success(AgentListUiState(agents = sampleAgents)),
        UiState.Success(AgentListUiState(agents = sampleAgents, searchQuery = "code")),
        UiState.Error("Failed to load agents"),
    )
}
