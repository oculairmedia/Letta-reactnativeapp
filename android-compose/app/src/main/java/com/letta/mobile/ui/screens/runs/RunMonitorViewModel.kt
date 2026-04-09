package com.letta.mobile.ui.screens.runs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letta.mobile.data.model.Run
import com.letta.mobile.data.model.RunListParams
import com.letta.mobile.data.repository.RunRepository
import com.letta.mobile.ui.common.UiState
import com.letta.mobile.util.mapErrorToUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@androidx.compose.runtime.Immutable
data class RunMonitorUiState(
    val runs: List<Run> = emptyList(),
    val searchQuery: String = "",
    val activeOnly: Boolean = false,
    val selectedRun: Run? = null,
)

@HiltViewModel
class RunMonitorViewModel @Inject constructor(
    private val runRepository: RunRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<RunMonitorUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<RunMonitorUiState>> = _uiState.asStateFlow()

    init {
        loadRuns()
    }

    fun loadRuns() {
        viewModelScope.launch {
            val current = (_uiState.value as? UiState.Success)?.data
            val activeOnly = current?.activeOnly ?: false
            val searchQuery = current?.searchQuery.orEmpty()
            val selectedRun = current?.selectedRun
            _uiState.value = UiState.Loading
            try {
                runRepository.refreshRuns(RunListParams(active = activeOnly))
                _uiState.value = UiState.Success(
                    RunMonitorUiState(
                        runs = runRepository.runs.value,
                        searchQuery = searchQuery,
                        activeOnly = activeOnly,
                        selectedRun = selectedRun,
                    )
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error(mapErrorToUserMessage(e, "Failed to load runs"))
            }
        }
    }

    fun updateSearchQuery(query: String) {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        _uiState.value = UiState.Success(current.copy(searchQuery = query))
    }

    fun toggleActiveOnly(value: Boolean) {
        val current = (_uiState.value as? UiState.Success)?.data ?: RunMonitorUiState()
        _uiState.value = UiState.Success(current.copy(activeOnly = value))
        loadRuns()
    }

    fun getFilteredRuns(): List<Run> {
        val current = (_uiState.value as? UiState.Success)?.data ?: return emptyList()
        if (current.searchQuery.isBlank()) return current.runs
        val q = current.searchQuery.trim().lowercase()
        return current.runs.filter { run ->
            run.id.lowercase().contains(q) ||
                run.agentId.lowercase().contains(q) ||
                (run.conversationId?.lowercase()?.contains(q) == true) ||
                (run.status?.lowercase()?.contains(q) == true) ||
                (run.stopReason?.lowercase()?.contains(q) == true)
        }
    }

    fun inspectRun(runId: String) {
        viewModelScope.launch {
            val current = (_uiState.value as? UiState.Success)?.data ?: return@launch
            try {
                val run = runRepository.getRun(runId)
                _uiState.value = UiState.Success(current.copy(selectedRun = run))
            } catch (e: Exception) {
                _uiState.value = UiState.Error(mapErrorToUserMessage(e, "Failed to load run details"))
            }
        }
    }

    fun clearSelectedRun() {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        _uiState.value = UiState.Success(current.copy(selectedRun = null))
    }
}
