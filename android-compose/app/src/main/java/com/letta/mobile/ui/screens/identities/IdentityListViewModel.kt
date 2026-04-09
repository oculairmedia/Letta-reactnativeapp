package com.letta.mobile.ui.screens.identities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letta.mobile.data.model.Identity
import com.letta.mobile.data.model.IdentityCreateParams
import com.letta.mobile.data.repository.IdentityRepository
import com.letta.mobile.ui.common.UiState
import com.letta.mobile.util.mapErrorToUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@androidx.compose.runtime.Immutable
data class IdentityListUiState(
    val identities: List<Identity> = emptyList(),
    val searchQuery: String = "",
    val selectedIdentity: Identity? = null,
)

@HiltViewModel
class IdentityListViewModel @Inject constructor(
    private val identityRepository: IdentityRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<IdentityListUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<IdentityListUiState>> = _uiState.asStateFlow()

    init {
        loadIdentities()
    }

    fun loadIdentities() {
        viewModelScope.launch {
            val current = (_uiState.value as? UiState.Success)?.data
            _uiState.value = UiState.Loading
            try {
                identityRepository.refreshIdentities()
                _uiState.value = UiState.Success(
                    IdentityListUiState(
                        identities = identityRepository.identities.value,
                        searchQuery = current?.searchQuery.orEmpty(),
                        selectedIdentity = current?.selectedIdentity,
                    )
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error(mapErrorToUserMessage(e, "Failed to load identities"))
            }
        }
    }

    fun updateSearchQuery(query: String) {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        _uiState.value = UiState.Success(current.copy(searchQuery = query))
    }

    fun getFilteredIdentities(): List<Identity> {
        val current = (_uiState.value as? UiState.Success)?.data ?: return emptyList()
        if (current.searchQuery.isBlank()) return current.identities
        val q = current.searchQuery.trim().lowercase()
        return current.identities.filter { identity ->
            identity.name.lowercase().contains(q) ||
                identity.identifierKey.lowercase().contains(q) ||
                identity.identityType.lowercase().contains(q)
        }
    }

    fun inspectIdentity(identityId: String) {
        viewModelScope.launch {
            val current = (_uiState.value as? UiState.Success)?.data ?: return@launch
            try {
                val identity = identityRepository.getIdentity(identityId)
                _uiState.value = UiState.Success(current.copy(selectedIdentity = identity))
            } catch (e: Exception) {
                _uiState.value = UiState.Error(mapErrorToUserMessage(e, "Failed to load identity details"))
            }
        }
    }

    fun clearSelectedIdentity() {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        _uiState.value = UiState.Success(current.copy(selectedIdentity = null))
    }

    fun createIdentity(params: IdentityCreateParams) {
        viewModelScope.launch {
            try {
                identityRepository.createIdentity(params)
                loadIdentities()
            } catch (e: Exception) {
                _uiState.value = UiState.Error(mapErrorToUserMessage(e, "Failed to create identity"))
            }
        }
    }

    fun deleteIdentity(identityId: String) {
        viewModelScope.launch {
            try {
                identityRepository.deleteIdentity(identityId)
                val current = (_uiState.value as? UiState.Success)?.data ?: return@launch
                _uiState.value = UiState.Success(
                    current.copy(
                        identities = current.identities.filterNot { it.id == identityId },
                        selectedIdentity = if (current.selectedIdentity?.id == identityId) null else current.selectedIdentity,
                    )
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error(mapErrorToUserMessage(e, "Failed to delete identity"))
            }
        }
    }
}
