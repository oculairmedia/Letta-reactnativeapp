package com.letta.mobile.ui.screens.agentlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letta.mobile.data.model.Agent
import com.letta.mobile.data.model.AgentCreateParams
import com.letta.mobile.data.repository.AgentRepository
import com.letta.mobile.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AgentListUiState(
    val agents: List<Agent> = emptyList(),
    val isRefreshing: Boolean = false,
    val searchQuery: String = ""
)

@HiltViewModel
class AgentListViewModel @Inject constructor(
    private val agentRepository: AgentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<AgentListUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<AgentListUiState>> = _uiState.asStateFlow()

    init {
        loadAgents()
    }

    fun loadAgents() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                agentRepository.refreshAgents()
                _uiState.value = UiState.Success(
                    AgentListUiState(agents = agentRepository.agents.value)
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load agents")
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val currentState = (_uiState.value as? UiState.Success)?.data
            if (currentState != null) {
                _uiState.value = UiState.Success(currentState.copy(isRefreshing = true))
            }
            try {
                agentRepository.refreshAgents()
                _uiState.value = UiState.Success(
                    AgentListUiState(agents = agentRepository.agents.value, isRefreshing = false)
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to refresh")
            }
        }
    }

    fun deleteAgent(agentId: String) {
        viewModelScope.launch {
            try {
                agentRepository.deleteAgent(agentId)
                loadAgents()
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to delete agent")
            }
        }
    }

    fun updateSearchQuery(query: String) {
        val currentState = (_uiState.value as? UiState.Success)?.data
        if (currentState != null) {
            _uiState.value = UiState.Success(currentState.copy(searchQuery = query))
        }
    }

    fun createAgent(name: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val agent = agentRepository.createAgent(AgentCreateParams(name = name))
                onSuccess(agent.id)
                loadAgents()
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to create agent")
            }
        }
    }
}
