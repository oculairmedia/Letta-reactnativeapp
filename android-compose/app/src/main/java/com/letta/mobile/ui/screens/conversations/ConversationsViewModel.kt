package com.letta.mobile.ui.screens.conversations

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letta.mobile.data.model.Agent
import com.letta.mobile.data.model.Conversation
import com.letta.mobile.data.repository.AgentRepository
import com.letta.mobile.data.repository.AllConversationsRepository
import com.letta.mobile.data.repository.ConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@androidx.compose.runtime.Immutable
data class ConversationDisplay(
    val conversation: Conversation,
    val agentName: String,
)

@androidx.compose.runtime.Immutable
data class ConversationsUiState(
    val conversations: List<ConversationDisplay> = emptyList(),
    val agents: List<Agent> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val error: String? = null,
)

@HiltViewModel
class ConversationsViewModel @Inject constructor(
    private val allConversationsRepository: AllConversationsRepository,
    private val conversationRepository: ConversationRepository,
    private val agentRepository: AgentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationsUiState())
    val uiState: StateFlow<ConversationsUiState> = _uiState.asStateFlow()

    private var agentNameCache = mutableMapOf<String, String>()

    init {
        val cachedAgents = agentRepository.agents.value
        if (cachedAgents.isNotEmpty()) {
            agentNameCache = cachedAgents.associate { it.id to it.name }.toMutableMap()
        }
        val cachedConversations = allConversationsRepository.conversations.value
        if (cachedConversations.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                conversations = cachedConversations.map { it.toDisplay() },
                agents = cachedAgents,
                isLoading = false,
            )
        }
        loadConversations()
    }

    fun loadConversations() {
        viewModelScope.launch {
            if (_uiState.value.conversations.isEmpty()) {
                _uiState.value = _uiState.value.copy(isLoading = true)
            }
            try {
                agentRepository.refreshAgents()
                agentNameCache = agentRepository.agents.value
                    .associate { it.id to it.name }
                    .toMutableMap()

                allConversationsRepository.refresh()
                _uiState.value = _uiState.value.copy(
                    conversations = allConversationsRepository.conversations.value.map { it.toDisplay() },
                    agents = agentRepository.agents.value,
                    isLoading = false,
                    error = null,
                )
            } catch (e: Exception) {
                Log.w("ConversationsVM", "Load failed", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = if (_uiState.value.conversations.isEmpty()) e.message else null,
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            try {
                allConversationsRepository.refresh()
                _uiState.value = _uiState.value.copy(
                    conversations = allConversationsRepository.conversations.value.map { it.toDisplay() },
                    agents = agentRepository.agents.value,
                    isRefreshing = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isRefreshing = false)
            }
        }
    }

    fun deleteConversation(conversationId: String) {
        allConversationsRepository.handleOptimisticDelete(conversationId)
        _uiState.value = _uiState.value.copy(
            conversations = _uiState.value.conversations.filter { it.conversation.id != conversationId }
        )
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun renameConversation(conversationId: String, agentId: String, newName: String) {
        viewModelScope.launch {
            try {
                conversationRepository.updateConversation(conversationId, agentId, newName)
                _uiState.value = _uiState.value.copy(
                    conversations = _uiState.value.conversations.map {
                        if (it.conversation.id == conversationId) {
                            it.copy(conversation = it.conversation.copy(summary = newName))
                        } else it
                    }
                )
            } catch (e: Exception) {
                Log.w("ConversationsVM", "Rename failed", e)
            }
        }
    }

    fun createConversation(agentId: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val conversation = conversationRepository.createConversation(agentId)
                onSuccess(conversation.id)
                allConversationsRepository.handleOptimisticUpdate(conversation)
                _uiState.value = _uiState.value.copy(
                    conversations = _uiState.value.conversations + conversation.toDisplay()
                )
            } catch (e: Exception) {
                Log.w("ConversationsVM", "Create failed", e)
            }
        }
    }

    private fun Conversation.toDisplay() = ConversationDisplay(
        conversation = this,
        agentName = agentNameCache[agentId] ?: agentId.take(8),
    )
}
