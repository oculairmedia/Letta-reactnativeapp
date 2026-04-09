package com.letta.mobile.ui.screens.blocks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letta.mobile.data.model.Block
import com.letta.mobile.data.repository.api.IBlockRepository
import com.letta.mobile.ui.common.UiState
import com.letta.mobile.util.mapErrorToUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@androidx.compose.runtime.Immutable
data class BlockLibraryUiState(
    val blocks: List<Block> = emptyList(),
    val filterLabel: String? = null,
    val filterTemplate: Boolean? = null,
)

@HiltViewModel
class BlockLibraryViewModel @Inject constructor(
    private val blockRepository: IBlockRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<BlockLibraryUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<BlockLibraryUiState>> = _uiState.asStateFlow()

    private var filterLabel: String? = null
    private var filterTemplate: Boolean? = null

    init {
        loadBlocks()
    }

    fun loadBlocks() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val blocks = blockRepository.listAllBlocks(
                    label = filterLabel,
                    isTemplate = filterTemplate,
                )
                _uiState.value = UiState.Success(
                    BlockLibraryUiState(
                        blocks = blocks,
                        filterLabel = filterLabel,
                        filterTemplate = filterTemplate,
                    )
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error(
                    mapErrorToUserMessage(e, "Failed to load blocks")
                )
            }
        }
    }

    fun setFilter(label: String?, isTemplate: Boolean?) {
        filterLabel = label
        filterTemplate = isTemplate
        loadBlocks()
    }
}
