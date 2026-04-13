package com.letta.mobile.ui.screens.projects

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.letta.mobile.R
import com.letta.mobile.data.model.ProjectSummary
import com.letta.mobile.ui.common.LocalSnackbarDispatcher
import com.letta.mobile.ui.common.UiState
import com.letta.mobile.ui.components.ActionSheet
import com.letta.mobile.ui.components.ActionSheetItem
import com.letta.mobile.ui.components.EmptyState
import com.letta.mobile.ui.components.ErrorContent
import com.letta.mobile.ui.icons.LettaIcons
import com.letta.mobile.ui.theme.LettaSpacing
import com.letta.mobile.ui.theme.LocalWindowSizeClass
import com.letta.mobile.ui.theme.isExpandedWidth
import com.letta.mobile.util.formatRelativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectHomeScreen(
    onNavigateToProjectChat: (project: ProjectSummary) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: ProjectHomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarDispatcher.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val newProjectPendingMessage = stringResource(R.string.screen_projects_new_project_pending)
    val missingAgentMessage = stringResource(R.string.screen_projects_missing_agent, "%s")
    val editPendingMessage = stringResource(R.string.screen_projects_edit_pending, "%s")
    val archivePendingMessage = stringResource(R.string.screen_projects_archive_pending, "%s")
    val deletePendingMessage = stringResource(R.string.screen_projects_delete_pending, "%s")

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = com.letta.mobile.ui.theme.LettaTopBarDefaults.scaffoldContainerColor(),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.screen_projects_title)) },
                scrollBehavior = scrollBehavior,
                colors = com.letta.mobile.ui.theme.LettaTopBarDefaults.largeTopAppBarColors(),
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(LettaIcons.Settings, contentDescription = stringResource(R.string.common_settings))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    snackbar.dispatch(newProjectPendingMessage)
                }
            ) {
                Icon(LettaIcons.Add, contentDescription = stringResource(R.string.screen_projects_new_project))
            }
        },
    ) { paddingValues ->
        when (val state = uiState) {
            is UiState.Loading -> {
                EmptyState(
                    icon = LettaIcons.Apps,
                    message = stringResource(R.string.common_loading),
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
                )
            }

            is UiState.Error -> {
                ErrorContent(
                    message = state.message,
                    onRetry = viewModel::refresh,
                    modifier = Modifier.padding(paddingValues),
                )
            }

            is UiState.Success -> {
                PullToRefreshBox(
                    isRefreshing = state.data.isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
                ) {
                    if (state.data.projects.isEmpty()) {
                        EmptyState(
                            icon = LettaIcons.Apps,
                            message = stringResource(R.string.screen_projects_empty),
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        val minTileWidth = if (LocalWindowSizeClass.current.isExpandedWidth) 220.dp else 150.dp
                        LazyVerticalGrid(
                            modifier = Modifier.fillMaxSize(),
                            columns = GridCells.Adaptive(minSize = minTileWidth),
                            contentPadding = PaddingValues(LettaSpacing.screenHorizontal),
                            verticalArrangement = Arrangement.spacedBy(LettaSpacing.cardGap),
                            horizontalArrangement = Arrangement.spacedBy(LettaSpacing.cardGap),
                        ) {
                            items(state.data.projects, key = { it.identifier }) { project ->
                                ProjectTile(
                                    project = project,
                                    onClick = {
                                        val agentId = project.lettaAgentId
                                        if (agentId.isNullOrBlank()) {
                                            snackbar.dispatch(missingAgentMessage.format(project.name))
                                        } else {
                                            onNavigateToProjectChat(project)
                                        }
                                    },
                                    onOpenActions = { viewModel.selectProject(project.identifier) },
                                )
                            }
                        }
                    }
                }

                val selectedProject = viewModel.currentProject()
                ActionSheet(
                    show = selectedProject != null,
                    onDismiss = { viewModel.selectProject(null) },
                    title = selectedProject?.name,
                ) {
                    val project = selectedProject ?: return@ActionSheet
                    ActionSheetItem(
                        text = stringResource(R.string.screen_projects_open_chat_action),
                        icon = LettaIcons.Chat,
                        onClick = {
                            viewModel.selectProject(null)
                            val agentId = project.lettaAgentId
                            if (agentId.isNullOrBlank()) {
                                snackbar.dispatch(missingAgentMessage.format(project.name))
                            } else {
                                onNavigateToProjectChat(project)
                            }
                        },
                    )
                    ActionSheetItem(
                        text = stringResource(R.string.action_edit),
                        icon = LettaIcons.Edit,
                        onClick = {
                            viewModel.selectProject(null)
                            snackbar.dispatch(editPendingMessage.format(project.name))
                        },
                    )
                    ActionSheetItem(
                        text = stringResource(R.string.screen_projects_archive_action),
                        icon = LettaIcons.Archive,
                        onClick = {
                            viewModel.selectProject(null)
                            snackbar.dispatch(archivePendingMessage.format(project.name))
                        },
                    )
                    ActionSheetItem(
                        text = stringResource(R.string.action_delete),
                        icon = LettaIcons.Delete,
                        onClick = {
                            viewModel.selectProject(null)
                            snackbar.dispatch(deletePendingMessage.format(project.name))
                        },
                        destructive = true,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProjectTile(
    project: ProjectSummary,
    onClick: () -> Unit,
    onOpenActions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val lastActivity = project.updatedAt ?: project.lastSyncAt ?: project.lastCheckedAt ?: project.lastScanAt
    val statusColor = when (project.status?.lowercase()) {
        "active" -> MaterialTheme.colorScheme.tertiary
        "archived" -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.primary
    }
    val initials = project.name
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString(separator = "") { it.take(1).uppercase() }
        .ifBlank { project.identifier.take(2).uppercase() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(182.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onOpenActions()
                },
            ),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Box(
                        modifier = Modifier.size(44.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }

                BadgedBox(
                    badge = {
                        val issueCount = project.beadsIssueCount ?: project.issueCount ?: 0
                        if (issueCount > 0) {
                            Badge {
                                Text(issueCount.toString())
                            }
                        }
                    }
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp),
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    shape = RoundedCornerShape(50),
                                    color = statusColor,
                                ) {}
                            }
                            Text(
                                text = project.status?.replaceFirstChar { it.uppercase() } ?: stringResource(R.string.common_unknown),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = project.identifier,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                project.filesystemPath?.let { path ->
                    Text(
                        text = path,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = if (lastActivity.isNullOrBlank()) {
                        stringResource(R.string.screen_projects_last_activity_unknown)
                    } else {
                        stringResource(
                            R.string.screen_projects_last_activity,
                            formatRelativeTime(lastActivity),
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
