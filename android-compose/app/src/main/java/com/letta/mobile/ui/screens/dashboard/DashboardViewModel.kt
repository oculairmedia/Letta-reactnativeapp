package com.letta.mobile.ui.screens.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letta.mobile.data.repository.AgentRepository
import com.letta.mobile.data.repository.AllConversationsRepository
import com.letta.mobile.data.repository.SettingsRepository
import com.letta.mobile.data.repository.ToolRepository
import com.letta.mobile.data.repository.api.IBlockRepository
import com.letta.mobile.domain.AdminAgentManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@androidx.compose.runtime.Immutable
data class DashboardUiState(
    val serverUrl: String = "",
    val isConnected: Boolean = false,
    val agentCount: Int? = null,
    val conversationCount: Int? = null,
    val toolCount: Int? = null,
    val blockCount: Int? = null,
    val adminAgentId: String? = null,
    val adminAgentName: String = "Letta Admin",
    val error: String? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val agentRepository: AgentRepository,
    private val allConversationsRepository: AllConversationsRepository,
    private val toolRepository: ToolRepository,
    private val blockRepository: IBlockRepository,
    private val settingsRepository: SettingsRepository,
    private val adminAgentManager: AdminAgentManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            serverUrl = settingsRepository.activeConfig.value?.serverUrl ?: "",
            adminAgentId = settingsRepository.adminAgentId.value,
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadProgressively()
    }

    fun loadProgressively() {
        viewModelScope.launch {
            try {
                val admin = adminAgentManager.ensureAdminAgent()
                _uiState.value = _uiState.value.copy(
                    adminAgentId = admin.id,
                    adminAgentName = admin.name,
                    isConnected = true,
                )
            } catch (e: Exception) {
                Log.w("DashboardVM", "Admin agent failed", e)
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }

        viewModelScope.launch {
            try {
                agentRepository.refreshAgents()
                _uiState.value = _uiState.value.copy(
                    agentCount = agentRepository.agents.value.size,
                    isConnected = true,
                )
            } catch (e: Exception) {
                Log.w("DashboardVM", "Agent count failed", e)
            }
        }

        viewModelScope.launch {
            try {
                allConversationsRepository.refresh()
                _uiState.value = _uiState.value.copy(
                    conversationCount = allConversationsRepository.conversations.value.size,
                )
            } catch (e: Exception) {
                Log.w("DashboardVM", "Conversation count failed", e)
            }
        }

        viewModelScope.launch {
            try {
                toolRepository.refreshTools()
                _uiState.value = _uiState.value.copy(
                    toolCount = toolRepository.getTools().first().size,
                )
            } catch (e: Exception) {
                Log.w("DashboardVM", "Tool count failed", e)
            }
        }

        viewModelScope.launch {
            try {
                val blocks = blockRepository.listAllBlocks()
                _uiState.value = _uiState.value.copy(
                    blockCount = blocks.size,
                )
            } catch (e: Exception) {
                Log.w("DashboardVM", "Block count failed", e)
            }
        }
    }
}
