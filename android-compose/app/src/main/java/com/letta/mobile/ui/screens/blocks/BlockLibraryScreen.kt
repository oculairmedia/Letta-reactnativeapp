package com.letta.mobile.ui.screens.blocks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.letta.mobile.R
import com.letta.mobile.data.model.Block
import com.letta.mobile.ui.common.UiState
import com.letta.mobile.ui.components.EmptyState
import com.letta.mobile.ui.components.ErrorContent
import com.letta.mobile.ui.components.ShimmerCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockLibraryScreen(
    onNavigateBack: () -> Unit,
    viewModel: BlockLibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedBlock by remember { mutableStateOf<Block?>(null) }
    var deleteTarget by remember { mutableStateOf<Block?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_blocks_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { paddingValues ->
        when (val state = uiState) {
            is UiState.Loading -> ShimmerCard(modifier = Modifier.padding(16.dp))
            is UiState.Error -> ErrorContent(
                message = state.message,
                onRetry = { viewModel.loadBlocks() },
                modifier = Modifier.padding(paddingValues),
            )
            is UiState.Success -> {
                val filteredBlocks = remember(state.data.blocks, state.data.searchQuery) {
                    viewModel.getFilteredBlocks()
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                ) {
                    OutlinedTextField(
                        value = state.data.searchQuery,
                        onValueChange = viewModel::updateSearchQuery,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text(stringResource(R.string.screen_blocks_search_hint)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                    )

                    if (filteredBlocks.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.Search,
                            message = if (state.data.searchQuery.isBlank()) {
                                stringResource(R.string.screen_blocks_empty)
                            } else {
                                stringResource(R.string.screen_blocks_empty_search, state.data.searchQuery)
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(filteredBlocks, key = { it.id }) { block ->
                                BlockLibraryCard(
                                    block = block,
                                    onInspect = { selectedBlock = block },
                                    onDelete = { deleteTarget = block },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedBlock?.let { block ->
        BlockDetailDialog(
            block = block,
            onDismiss = { selectedBlock = null },
        )
    }

    deleteTarget?.let { block ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.screen_blocks_delete_title)) },
            text = { Text(stringResource(R.string.screen_blocks_delete_confirm, block.label ?: stringResource(R.string.common_unknown))) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBlock(block.id)
                        deleteTarget = null
                        if (selectedBlock?.id == block.id) selectedBlock = null
                    },
                ) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun BlockLibraryCard(
    block: Block,
    onInspect: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        onClick = onInspect,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = block.label ?: stringResource(R.string.common_unknown),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    block.description?.takeIf { it.isNotBlank() }?.let { description ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, stringResource(R.string.action_delete))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                block.limit?.let { limit ->
                    AssistChip(onClick = {}, label = { Text(stringResource(R.string.screen_blocks_limit_chip, limit)) })
                }
                block.isTemplate?.takeIf { it }?.let {
                    AssistChip(onClick = {}, label = { Text(stringResource(R.string.screen_agent_edit_block_template)) })
                }
                block.readOnly?.takeIf { it }?.let {
                    AssistChip(onClick = {}, label = { Text(stringResource(R.string.screen_agent_edit_block_read_only)) })
                }
            }
        }
    }
}

@Composable
private fun BlockDetailDialog(
    block: Block,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(block.label ?: stringResource(R.string.common_unknown)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                block.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Text(description, style = MaterialTheme.typography.bodyMedium)
                }
                HorizontalDivider()
                block.limit?.let { limit ->
                    Text(
                        text = stringResource(R.string.screen_blocks_limit_label, limit),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                block.createdAt?.let { createdAt ->
                    Text(
                        text = stringResource(R.string.screen_blocks_created_label, createdAt),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                block.updatedAt?.let { updatedAt ->
                    Text(
                        text = stringResource(R.string.screen_blocks_updated_label, updatedAt),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                HorizontalDivider()
                Text(
                    text = block.value,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
    )
}
