package com.letta.mobile.ui.screens.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letta.mobile.data.model.Conversation
import com.letta.mobile.data.repository.AllConversationsRepository
import com.letta.mobile.data.repository.ConversationRepository
import com.letta.mobile.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConversationsUiState(
    val conversations: List<Conversation> = emptyList(),
    val isRefreshing: Boolean = false,
    val searchQuery: String = ""
)

@HiltViewModel
class ConversationsViewModel @Inject constructor(
    private val allConversationsRepository: AllConversationsRepository,
    private val conversationRepository: ConversationRepository,
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<UiState<ConversationsUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<ConversationsUiState>> = _uiState.asStateFlow()

    init {
        loadConversations()
    }

    fun loadConversations() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                allConversationsRepository.refresh()
                _uiState.value = UiState.Success(ConversationsUiState(conversations = allConversationsRepository.conversations.value))
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load conversations")
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
                allConversationsRepository.refresh()
                _uiState.value = UiState.Success(ConversationsUiState(conversations = allConversationsRepository.conversations.value, isRefreshing = false))
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to refresh")
            }
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            try {
                allConversationsRepository.handleOptimisticDelete(conversationId)
                val currentState = (_uiState.value as? UiState.Success)?.data
                if (currentState != null) {
                    _uiState.value = UiState.Success(currentState.copy(conversations = allConversationsRepository.conversations.value))
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to delete conversation")
            }
        }
    }

    fun updateSearchQuery(query: String) {
        val currentState = (_uiState.value as? UiState.Success)?.data
        if (currentState != null) {
            _uiState.value = UiState.Success(currentState.copy(searchQuery = query))
        }
    }

    fun createConversation(agentId: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val conversation = conversationRepository.createConversation(agentId)
                onSuccess(conversation.id)
                allConversationsRepository.handleOptimisticUpdate(conversation)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to create conversation")
            }
        }
    }
}
