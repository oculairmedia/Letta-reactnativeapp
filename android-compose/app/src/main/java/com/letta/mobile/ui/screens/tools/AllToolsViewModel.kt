package com.letta.mobile.ui.screens.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letta.mobile.data.model.Tool
import com.letta.mobile.data.repository.ToolRepository
import com.letta.mobile.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@androidx.compose.runtime.Immutable
data class AllToolsUiState(
    val tools: List<Tool> = emptyList(),
    val searchQuery: String = "",
)

@HiltViewModel
class AllToolsViewModel @Inject constructor(
    private val toolRepository: ToolRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<AllToolsUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<AllToolsUiState>> = _uiState.asStateFlow()

    init {
        loadTools()
    }

    fun createTool(name: String, sourceCode: String) {
        viewModelScope.launch {
            try {
                toolRepository.upsertTool(com.letta.mobile.data.model.ToolCreateParams(name = name, sourceCode = sourceCode))
                loadTools()
            } catch (e: Exception) {
                _uiState.value = UiState.Error(com.letta.mobile.util.mapErrorToUserMessage(e, "Failed to create tool"))
            }
        }
    }

    fun updateSearchQuery(query: String) {
        val currentState = (_uiState.value as? UiState.Success)?.data ?: return
        _uiState.value = UiState.Success(currentState.copy(searchQuery = query))
    }

    fun getFilteredTools(): List<Tool> {
        val currentState = (_uiState.value as? UiState.Success)?.data ?: return emptyList()
        if (currentState.searchQuery.isBlank()) return currentState.tools
        val q = currentState.searchQuery.trim().lowercase()
        return currentState.tools.filter { tool ->
            tool.name.lowercase().contains(q) ||
                (tool.description?.lowercase()?.contains(q) == true) ||
                (tool.toolType?.lowercase()?.contains(q) == true) ||
                (tool.sourceType?.lowercase()?.contains(q) == true) ||
                (tool.tags?.any { it.lowercase().contains(q) } == true)
        }
    }

    fun loadTools() {
        viewModelScope.launch {
            val searchQuery = (_uiState.value as? UiState.Success)?.data?.searchQuery.orEmpty()
            _uiState.value = UiState.Loading
            try {
                toolRepository.refreshTools()
                val tools = toolRepository.getTools().first()
                _uiState.value = UiState.Success(AllToolsUiState(tools = tools, searchQuery = searchQuery))
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load tools")
            }
        }
    }
}
