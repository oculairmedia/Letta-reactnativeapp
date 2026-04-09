package com.letta.mobile.ui.screens.runs

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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.letta.mobile.R
import com.letta.mobile.data.model.Run
import com.letta.mobile.ui.common.UiState
import com.letta.mobile.ui.components.EmptyState
import com.letta.mobile.ui.components.ErrorContent
import com.letta.mobile.ui.components.ShimmerCard
import com.letta.mobile.util.formatRelativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunMonitorScreen(
    onNavigateBack: () -> Unit,
    viewModel: RunMonitorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_runs_title)) },
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
                onRetry = { viewModel.loadRuns() },
                modifier = Modifier.padding(paddingValues),
            )
            is UiState.Success -> {
                val filteredRuns = remember(state.data.runs, state.data.searchQuery) {
                    viewModel.getFilteredRuns()
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
                        placeholder = { Text(stringResource(R.string.screen_runs_search_hint)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.screen_runs_active_only), style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = state.data.activeOnly,
                            onCheckedChange = viewModel::toggleActiveOnly,
                        )
                    }

                    if (filteredRuns.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.AccessTime,
                            message = if (state.data.searchQuery.isBlank()) {
                                stringResource(R.string.screen_runs_empty)
                            } else {
                                stringResource(R.string.screen_runs_empty_search, state.data.searchQuery)
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(filteredRuns, key = { it.id }) { run ->
                                RunCard(
                                    run = run,
                                    onInspect = { viewModel.inspectRun(run.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val selectedRun = (uiState as? UiState.Success)?.data?.selectedRun
    selectedRun?.let { run ->
        RunDetailDialog(
            run = run,
            onDismiss = { viewModel.clearSelectedRun() },
        )
    }
}

@Composable
private fun RunCard(
    run: Run,
    onInspect: () -> Unit,
) {
    Card(
        onClick = onInspect,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = run.id,
                style = MaterialTheme.typography.titleSmall,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                run.status?.let { status ->
                    AssistChip(onClick = {}, label = { Text(status) })
                }
                if (run.background == true) {
                    AssistChip(onClick = {}, label = { Text(stringResource(R.string.screen_runs_background_chip)) })
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.screen_runs_agent_label, run.agentId),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            run.conversationId?.let { conversationId ->
                Text(
                    text = stringResource(R.string.screen_runs_conversation_label, conversationId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            run.createdAt?.let { createdAt ->
                Text(
                    text = stringResource(R.string.screen_runs_created_label, formatRelativeTime(createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RunDetailDialog(
    run: Run,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(run.id, fontFamily = FontFamily.Monospace) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                run.status?.let { Text(stringResource(R.string.screen_runs_status_label, it)) }
                run.stopReason?.let { Text(stringResource(R.string.screen_runs_stop_reason_label, it)) }
                Text(stringResource(R.string.screen_runs_agent_label, run.agentId))
                run.conversationId?.let { Text(stringResource(R.string.screen_runs_conversation_label, it)) }
                run.createdAt?.let { Text(stringResource(R.string.screen_runs_created_exact_label, it)) }
                run.completedAt?.let { Text(stringResource(R.string.screen_runs_completed_label, it)) }
                run.callbackUrl?.let { Text(stringResource(R.string.screen_runs_callback_label, it)) }
                run.callbackStatusCode?.let { Text(stringResource(R.string.screen_runs_callback_status_label, it)) }
                run.totalDurationNs?.let { Text(stringResource(R.string.screen_runs_total_duration_label, it)) }
                run.ttftNs?.let { Text(stringResource(R.string.screen_runs_ttft_label, it)) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
    )
}
