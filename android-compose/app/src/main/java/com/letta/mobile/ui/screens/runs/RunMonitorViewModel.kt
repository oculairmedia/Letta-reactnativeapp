package com.letta.mobile.ui.screens.runs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letta.mobile.data.model.LettaMessage
import com.letta.mobile.data.model.Run
import com.letta.mobile.data.model.RunListParams
import com.letta.mobile.data.model.RunMetrics
import com.letta.mobile.data.model.RunStep
import com.letta.mobile.data.model.UsageStatistics
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
    val selectedRunMessages: List<LettaMessage> = emptyList(),
    val selectedRunUsage: UsageStatistics? = null,
    val selectedRunMetrics: RunMetrics? = null,
    val selectedRunSteps: List<RunStep> = emptyList(),
    val operationError: String? = null,
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
                        selectedRunMessages = current?.selectedRunMessages.orEmpty(),
                        selectedRunUsage = current?.selectedRunUsage,
                        selectedRunMetrics = current?.selectedRunMetrics,
                        selectedRunSteps = current?.selectedRunSteps.orEmpty(),
                        operationError = current?.operationError,
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
                val messages = runRepository.getRunMessages(runId)
                val usage = runRepository.getRunUsage(runId)
                val metrics = runRepository.getRunMetrics(runId)
                val steps = runRepository.getRunSteps(runId)
                _uiState.value = UiState.Success(
                    current.copy(
                        selectedRun = run,
                        selectedRunMessages = messages,
                        selectedRunUsage = usage,
                        selectedRunMetrics = metrics,
                        selectedRunSteps = steps,
                        operationError = null,
                    )
                )
            } catch (e: Exception) {
                setOperationError(mapErrorToUserMessage(e, "Failed to load run details"))
            }
        }
    }

    fun cancelRun(runId: String) {
        viewModelScope.launch {
            val current = (_uiState.value as? UiState.Success)?.data ?: return@launch
            val selectedRun = current.selectedRun ?: current.runs.firstOrNull { it.id == runId } ?: return@launch
            try {
                val refreshedRun = runRepository.cancelRun(selectedRun)
                val refreshedRuns = current.runs.map { if (it.id == refreshedRun.id) refreshedRun else it }
                    .filterNot { current.activeOnly && it.status !in setOf("created", "running") }
                _uiState.value = UiState.Success(
                    current.copy(
                        runs = refreshedRuns,
                        selectedRun = if (current.activeOnly && refreshedRun.status !in setOf("created", "running")) null else refreshedRun,
                        operationError = null,
                    )
                )
            } catch (e: Exception) {
                setOperationError(mapErrorToUserMessage(e, "Failed to cancel run"))
            }
        }
    }

    fun deleteRun(runId: String) {
        viewModelScope.launch {
            val current = (_uiState.value as? UiState.Success)?.data ?: return@launch
            try {
                runRepository.deleteRun(runId)
                _uiState.value = UiState.Success(
                    current.copy(
                        runs = current.runs.filterNot { it.id == runId },
                        selectedRun = if (current.selectedRun?.id == runId) null else current.selectedRun,
                        selectedRunMessages = if (current.selectedRun?.id == runId) emptyList() else current.selectedRunMessages,
                        selectedRunUsage = if (current.selectedRun?.id == runId) null else current.selectedRunUsage,
                        selectedRunMetrics = if (current.selectedRun?.id == runId) null else current.selectedRunMetrics,
                        selectedRunSteps = if (current.selectedRun?.id == runId) emptyList() else current.selectedRunSteps,
                        operationError = null,
                    )
                )
            } catch (e: Exception) {
                setOperationError(mapErrorToUserMessage(e, "Failed to delete run"))
            }
        }
    }

    fun clearSelectedRun() {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        _uiState.value = UiState.Success(
            current.copy(
                selectedRun = null,
                selectedRunMessages = emptyList(),
                selectedRunUsage = null,
                selectedRunMetrics = null,
                selectedRunSteps = emptyList(),
            )
        )
    }

    fun clearOperationError() {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        _uiState.value = UiState.Success(current.copy(operationError = null))
    }

    private fun setOperationError(message: String) {
        val current = (_uiState.value as? UiState.Success)?.data
        if (current != null) {
            _uiState.value = UiState.Success(current.copy(operationError = message))
        } else {
            _uiState.value = UiState.Error(message)
        }
    }
}
