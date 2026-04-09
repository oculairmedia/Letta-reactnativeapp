package com.letta.mobile.ui.screens.identities

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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.letta.mobile.data.model.Identity
import com.letta.mobile.data.model.IdentityCreateParams
import com.letta.mobile.ui.common.UiState
import com.letta.mobile.ui.components.EmptyState
import com.letta.mobile.ui.components.ErrorContent
import com.letta.mobile.ui.components.ShimmerCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentityListScreen(
    onNavigateBack: () -> Unit,
    viewModel: IdentityListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Identity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_identities_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.action_back))
                    }
                },
            )
        },
        floatingActionButton = {
            androidx.compose.material3.FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, stringResource(R.string.screen_identities_add_title))
            }
        },
    ) { paddingValues ->
        when (val state = uiState) {
            is UiState.Loading -> ShimmerCard(modifier = Modifier.padding(16.dp))
            is UiState.Error -> ErrorContent(
                message = state.message,
                onRetry = { viewModel.loadIdentities() },
                modifier = Modifier.padding(paddingValues),
            )
            is UiState.Success -> {
                val filteredIdentities = remember(state.data.identities, state.data.searchQuery) {
                    viewModel.getFilteredIdentities()
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
                        placeholder = { Text(stringResource(R.string.screen_identities_search_hint)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                    )

                    if (filteredIdentities.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.AccountCircle,
                            message = if (state.data.searchQuery.isBlank()) {
                                stringResource(R.string.screen_identities_empty)
                            } else {
                                stringResource(R.string.screen_identities_empty_search, state.data.searchQuery)
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(filteredIdentities, key = { it.id }) { identity ->
                                IdentityCard(
                                    identity = identity,
                                    onInspect = { viewModel.inspectIdentity(identity.id) },
                                    onDelete = { deleteTarget = identity },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val selectedIdentity = (uiState as? UiState.Success)?.data?.selectedIdentity
    selectedIdentity?.let { identity ->
        IdentityDetailDialog(
            identity = identity,
            onDismiss = { viewModel.clearSelectedIdentity() },
        )
    }

    if (showCreateDialog) {
        CreateIdentityDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { params ->
                viewModel.createIdentity(params)
                showCreateDialog = false
            },
        )
    }

    deleteTarget?.let { identity ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.screen_identities_delete_title)) },
            text = { Text(stringResource(R.string.screen_identities_delete_confirm, identity.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteIdentity(identity.id)
                        deleteTarget = null
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
private fun IdentityCard(
    identity: Identity,
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
                    Text(identity.name, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = identity.identifierKey,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, stringResource(R.string.action_delete))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = {}, label = { Text(identity.identityType) })
                if (identity.properties.isNotEmpty()) {
                    AssistChip(onClick = {}, label = { Text(stringResource(R.string.screen_identities_properties_chip, identity.properties.size)) })
                }
            }
        }
    }
}

@Composable
private fun IdentityDetailDialog(
    identity: Identity,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(identity.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.screen_identities_identifier_label, identity.identifierKey), fontFamily = FontFamily.Monospace)
                Text(stringResource(R.string.screen_identities_type_label, identity.identityType))
                identity.projectId?.let { Text(stringResource(R.string.screen_identities_project_label, it)) }
                identity.organizationId?.let { Text(stringResource(R.string.screen_identities_organization_label, it)) }
                Text(stringResource(R.string.screen_identities_agent_count_label, identity.agentIds.size))
                Text(stringResource(R.string.screen_identities_block_count_label, identity.blockIds.size))
                if (identity.properties.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.screen_identities_properties_title), style = MaterialTheme.typography.labelLarge)
                    identity.properties.forEach { property ->
                        Text(
                            text = "${property.key}: ${property.value}",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
    )
}

@Composable
private fun CreateIdentityDialog(
    onDismiss: () -> Unit,
    onCreate: (IdentityCreateParams) -> Unit,
) {
    var identifierKey by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var identityType by remember { mutableStateOf("user") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.screen_identities_add_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = identifierKey,
                    onValueChange = { identifierKey = it },
                    label = { Text(stringResource(R.string.screen_identities_identifier_key_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.common_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = identityType,
                    onValueChange = { identityType = it },
                    label = { Text(stringResource(R.string.screen_identities_type_input_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCreate(
                        IdentityCreateParams(
                            identifierKey = identifierKey,
                            name = name,
                            identityType = identityType,
                        )
                    )
                },
                enabled = identifierKey.isNotBlank() && name.isNotBlank() && identityType.isNotBlank(),
            ) {
                Text(stringResource(R.string.action_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
