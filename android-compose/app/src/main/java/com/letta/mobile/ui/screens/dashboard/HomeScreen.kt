package com.letta.mobile.ui.screens.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.runtime.key
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.letta.mobile.R
import com.letta.mobile.data.model.Agent
import com.letta.mobile.data.model.Block
import com.letta.mobile.data.model.ParsedSearchMessage
import com.letta.mobile.data.model.Tool
import com.letta.mobile.ui.components.ActionSheet
import com.letta.mobile.ui.components.ActionSheetItem
import com.letta.mobile.ui.components.ExpandableTitleSearch
import com.letta.mobile.ui.components.LettaInputBar
import com.letta.mobile.ui.icons.LettaIcons
import com.letta.mobile.ui.theme.LettaSpacing
import com.letta.mobile.ui.theme.LocalWindowSizeClass
import com.letta.mobile.ui.theme.customColors
import com.letta.mobile.ui.theme.isExpandedWidth
import com.letta.mobile.ui.theme.statValue
import kotlinx.collections.immutable.ImmutableList
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAgents: () -> Unit,
    onNavigateToConversations: () -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToBlocks: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToChat: (agentId: String, initialMessage: String?) -> Unit,
    onNavigateToChatMessage: (agentId: String, conversationId: String, messageId: String) -> Unit,
    onNavigateToEditAgent: (agentId: String) -> Unit,
    onNavigateToUsage: () -> Unit,
    onNavigateToTemplates: () -> Unit = {},
    onNavigateToArchives: () -> Unit = {},
    onNavigateToFolders: () -> Unit = {},
    onNavigateToGroups: () -> Unit = {},
    onNavigateToProviders: () -> Unit = {},
    onNavigateToIdentities: () -> Unit = {},
    onNavigateToSchedules: () -> Unit = {},
    onNavigateToRuns: () -> Unit = {},
    onNavigateToJobs: () -> Unit = {},
    onNavigateToMessageBatches: () -> Unit = {},
    onNavigateToMcp: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToBotSettings: () -> Unit = {},
    onNavigateToProjects: () -> Unit = {},
    onNavigateToModels: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    fun shortcutNavigator(shortcut: DashboardShortcut): () -> Unit = when (shortcut) {
        DashboardShortcut.CONVERSATIONS -> onNavigateToConversations
        DashboardShortcut.AGENTS -> onNavigateToAgents
        DashboardShortcut.TOOLS -> onNavigateToTools
        DashboardShortcut.BLOCKS -> onNavigateToBlocks
        DashboardShortcut.TEMPLATES -> onNavigateToTemplates
        DashboardShortcut.ARCHIVES -> onNavigateToArchives
        DashboardShortcut.FOLDERS -> onNavigateToFolders
        DashboardShortcut.GROUPS -> onNavigateToGroups
        DashboardShortcut.PROVIDERS -> onNavigateToProviders
        DashboardShortcut.IDENTITIES -> onNavigateToIdentities
        DashboardShortcut.SCHEDULES -> onNavigateToSchedules
        DashboardShortcut.RUNS -> onNavigateToRuns
        DashboardShortcut.JOBS -> onNavigateToJobs
        DashboardShortcut.MESSAGE_BATCHES -> onNavigateToMessageBatches
        DashboardShortcut.MCP_SERVERS -> onNavigateToMcp
        DashboardShortcut.BOT_SETTINGS -> onNavigateToBotSettings
        DashboardShortcut.PROJECTS -> onNavigateToProjects
        DashboardShortcut.MODELS -> onNavigateToModels
        DashboardShortcut.SETTINGS -> onNavigateToSettings
        DashboardShortcut.ABOUT -> onNavigateToAbout
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Letta",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                    )

                    var previousGroup: DashboardShortcut.Group? = null
                    DashboardShortcut.entries.forEach { shortcut ->
                        if (previousGroup != null && shortcut.group != previousGroup) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 28.dp),
                            )
                        }
                        previousGroup = shortcut.group

                        key(shortcut) {
                            val isPinned = shortcut in uiState.pinnedShortcuts
                            val context = LocalContext.current
                            val label = stringResource(shortcut.labelResId)

                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(28.dp))
                                    .combinedClickable(
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                            shortcutNavigator(shortcut)()
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            if (isPinned) {
                                                viewModel.unpinShortcut(shortcut)
                                                android.widget.Toast
                                                    .makeText(context, "$label unpinned", android.widget.Toast.LENGTH_SHORT)
                                                    .show()
                                            } else {
                                                viewModel.pinShortcut(shortcut)
                                                android.widget.Toast
                                                    .makeText(context, "$label pinned", android.widget.Toast.LENGTH_SHORT)
                                                    .show()
                                            }
                                        },
                                    )
                                    .padding(start = 16.dp, end = 24.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    shortcut.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
    ) {
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = com.letta.mobile.ui.theme.LettaTopBarDefaults.scaffoldContainerColor(),
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    ExpandableTitleSearch(
                        query = uiState.searchQuery,
                        onQueryChange = viewModel::updateSearchQuery,
                        onClear = viewModel::clearSearch,
                        expanded = isSearchExpanded,
                        onExpandedChange = { isSearchExpanded = it },
                        placeholder = stringResource(R.string.screen_home_search_placeholder),
                        openSearchContentDescription = stringResource(R.string.action_search),
                        closeSearchContentDescription = stringResource(R.string.action_close),
                        titleContent = {
                            Text("Letta")
                            if (uiState.isConnected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    LettaIcons.Circle,
                                    contentDescription = "Connected",
                                    tint = MaterialTheme.customColors.onlineColor,
                                    modifier = Modifier.size(8.dp),
                                )
                            }
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(LettaIcons.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(LettaIcons.Settings, contentDescription = "Settings")
                    }
                },
                colors = com.letta.mobile.ui.theme.LettaTopBarDefaults.largeTopAppBarColors(),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        HomeContent(
            state = uiState,
            onNavigateToAgents = onNavigateToAgents,
            onNavigateToConversations = onNavigateToConversations,
            onNavigateToTools = onNavigateToTools,
            onNavigateToBlocks = onNavigateToBlocks,
            onNavigateToChat = onNavigateToChat,
            onNavigateToChatMessage = onNavigateToChatMessage,
            onNavigateToEditAgent = onNavigateToEditAgent,
            onNavigateToUsage = onNavigateToUsage,
            onClearFavorite = viewModel::clearFavorite,
            onUnpinAgent = viewModel::unpinAgent,
            onShortcutClick = { shortcut -> shortcutNavigator(shortcut)() },
            onUnpinShortcut = viewModel::unpinShortcut,
            onReorderShortcuts = viewModel::reorderShortcuts,
            modifier = Modifier.padding(paddingValues),
        )
    }
    }
}

@Composable
private fun HomeContent(
    state: DashboardUiState,
    onNavigateToAgents: () -> Unit,
    onNavigateToConversations: () -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToBlocks: () -> Unit,
    onNavigateToChat: (String, String?) -> Unit,
    onNavigateToChatMessage: (String, String, String) -> Unit,
    onNavigateToEditAgent: (String) -> Unit,
    onNavigateToUsage: () -> Unit,
    onClearFavorite: () -> Unit,
    onUnpinAgent: (String) -> Unit,
    onShortcutClick: (DashboardShortcut) -> Unit,
    onUnpinShortcut: (DashboardShortcut) -> Unit,
    onReorderShortcuts: (List<DashboardShortcut>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().imePadding()) {
        state.error?.let { error ->
            androidx.compose.material3.Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LettaSpacing.screenHorizontal)
                    .padding(bottom = LettaSpacing.cardGap),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LettaSpacing.screenHorizontal)
                .padding(bottom = LettaSpacing.cardGap),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.serverUrl,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (state.isSearchActive) {
            SearchResultsContent(
                agentResults = state.agentResults,
                messageResults = state.messageResults,
                toolResults = state.toolResults,
                blockResults = state.blockResults,
                isSearching = state.isSearching,
                searchQuery = state.searchQuery,
                onAgentClick = { agentId -> onNavigateToChat(agentId, null) },
                onMessageClick = { parsed ->
                    val agentId = parsed.agentId ?: return@SearchResultsContent
                    val convId = parsed.conversationId
                    val msgId = parsed.messageId
                    if (convId != null && msgId != null) {
                        onNavigateToChatMessage(agentId, convId, msgId)
                    } else {
                        onNavigateToChat(agentId, null)
                    }
                },
                onToolClick = { onNavigateToTools() },
                onBlockClick = { onNavigateToBlocks() },
                modifier = Modifier.weight(1f),
            )
        } else {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(LettaSpacing.cardGap),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = LettaSpacing.screenHorizontal),
                    horizontalArrangement = Arrangement.spacedBy(LettaSpacing.cardGap),
                ) {
                    StatCard(
                        label = "Agents",
                        value = state.agentCount?.toString(),
                        icon = LettaIcons.People,
                        onClick = onNavigateToAgents,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = "Chats",
                        value = state.conversationCount?.toString(),
                        icon = LettaIcons.Chat,
                        onClick = onNavigateToConversations,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = "Tools",
                        value = state.toolCount?.toString(),
                        icon = LettaIcons.Tool,
                        onClick = onNavigateToTools,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = "Blocks",
                        value = state.blockCount?.toString(),
                        icon = LettaIcons.ViewModule,
                        onClick = onNavigateToBlocks,
                        modifier = Modifier.weight(1f),
                    )
                }

                val isWide = LocalWindowSizeClass.current.isExpandedWidth

                if (isWide) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = LettaSpacing.screenHorizontal),
                        horizontalArrangement = Arrangement.spacedBy(LettaSpacing.cardGap),
                    ) {
                        UsageAnalyticsCard(
                            usageSummary = state.usageSummary,
                            isLoading = state.isUsageLoading,
                            onClick = onNavigateToUsage,
                            modifier = Modifier.weight(1f),
                        )
                        FavoriteAgentCard(
                            favoriteAgentId = state.favoriteAgentId,
                            favoriteAgentName = state.favoriteAgentName,
                            onNavigateToChat = { onNavigateToChat(it, null) },
                            onSetFavorite = onNavigateToAgents,
                            onClearFavorite = onClearFavorite,
                            onConfigure = { id -> onNavigateToEditAgent(id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    UsageAnalyticsCard(
                        usageSummary = state.usageSummary,
                        isLoading = state.isUsageLoading,
                        onClick = onNavigateToUsage,
                        modifier = Modifier.padding(horizontal = LettaSpacing.screenHorizontal),
                    )

                    FavoriteAgentCard(
                        favoriteAgentId = state.favoriteAgentId,
                        favoriteAgentName = state.favoriteAgentName,
                        onNavigateToChat = { onNavigateToChat(it, null) },
                        onSetFavorite = onNavigateToAgents,
                        onClearFavorite = onClearFavorite,
                        onConfigure = { id -> onNavigateToEditAgent(id) },
                    )
                }

                if (state.pinnedShortcuts.isNotEmpty()) {
                    Column(
                        modifier = Modifier.padding(horizontal = LettaSpacing.screenHorizontal),
                    ) {
                        Text(
                            text = stringResource(R.string.screen_home_shortcuts_title),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        ReorderableWidgetGrid(
                            shortcuts = state.pinnedShortcuts,
                            state = state,
                            onShortcutClick = onShortcutClick,
                            onUnpinShortcut = onUnpinShortcut,
                            onReorder = onReorderShortcuts,
                            columns = if (isWide) 3 else 2,
                        )
                    }
                }

                if (state.pinnedAgents.isNotEmpty()) {
                    val agentColumns = if (isWide) 3 else 2
                    Column(
                        modifier = Modifier.padding(horizontal = LettaSpacing.screenHorizontal),
                    ) {
                        state.pinnedAgents.chunked(agentColumns).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(LettaSpacing.cardGap),
                            ) {
                                row.forEach { pinned ->
                                    PinnedAgentCard(
                                        name = pinned.name,
                                        onClick = { onNavigateToChat(pinned.id, null) },
                                        onUnpin = { onUnpinAgent(pinned.id) },
                                        onConfigure = { onNavigateToEditAgent(pinned.id) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                repeat(agentColumns - row.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                            Spacer(modifier = Modifier.height(LettaSpacing.cardGap))
                        }
                    }
                }
            }

            if (state.favoriteAgentId != null) {
                var homeChatText by remember { mutableStateOf("") }
                LettaInputBar(
                    text = homeChatText,
                    onTextChange = { homeChatText = it },
                    onSend = { message ->
                        onNavigateToChat(state.favoriteAgentId, message)
                        homeChatText = ""
                    },
                    placeholder = stringResource(R.string.screen_home_chat_placeholder),
                    sendContentDescription = stringResource(R.string.action_send_message),
                    maxLines = 1,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun UsageAnalyticsCard(
    usageSummary: DashboardUsageSummary?,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColors = MaterialTheme.customColors
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = accentColors.freshAccentContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.screen_home_usage_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.screen_home_usage_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = LettaIcons.Sparkles,
                    contentDescription = null,
                    tint = accentColors.freshAccent,
                )
                Icon(
                    imageVector = LettaIcons.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }

            when {
                isLoading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        ContainedLoadingIndicator()
                    }
                }

                usageSummary == null || usageSummary.sampledSteps == 0 -> {
                    Text(
                        text = stringResource(R.string.screen_home_usage_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        UsageMetricCard(
                            label = stringResource(R.string.screen_home_usage_total_label),
                            value = formatNumber(usageSummary.totalTokens),
                            icon = LettaIcons.Database,
                            modifier = Modifier.weight(1f),
                        )
                        UsageMetricCard(
                            label = stringResource(R.string.screen_home_usage_hourly_label),
                            value = formatNumber(usageSummary.averageTokensPerHour),
                            icon = LettaIcons.AccessTime,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    HorizontalDivider()

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.screen_home_usage_model_split_title),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        usageSummary.modelUsage.take(5).forEachIndexed { index, modelUsage ->
                            ModelUsageRow(modelUsage = modelUsage)
                            if (index < usageSummary.modelUsage.take(5).lastIndex) {
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UsageMetricCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    val accentColors = MaterialTheme.customColors
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = accentColors.freshAccentContainer,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColors.freshAccent,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, style = MaterialTheme.typography.statValue)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ModelUsageRow(
    modelUsage: ModelTokenUsage,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = modelUsage.model,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.screen_home_usage_model_tokens_label, formatNumber(modelUsage.totalTokens)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.screen_home_usage_model_share_label, modelUsage.sharePercent),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteAgentCard(
    favoriteAgentId: String?,
    favoriteAgentName: String?,
    onNavigateToChat: (String) -> Unit,
    onSetFavorite: () -> Unit,
    onClearFavorite: () -> Unit,
    onConfigure: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (favoriteAgentId != null && favoriteAgentName != null) {
        var showMenu by remember { mutableStateOf(false) }
        val haptic = LocalHapticFeedback.current

        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = LettaSpacing.screenHorizontal)
                .combinedClickable(
                    onClick = { onNavigateToChat(favoriteAgentId) },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMenu = true
                    },
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    LettaIcons.Agent,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = favoriteAgentName,
                    style = MaterialTheme.typography.statValue,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.screen_home_favorite_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        ActionSheet(
            show = showMenu,
            onDismiss = { showMenu = false },
            title = favoriteAgentName,
        ) {
            ActionSheetItem(
                text = stringResource(R.string.action_configure_agent),
                icon = LettaIcons.Edit,
                onClick = { showMenu = false; onConfigure(favoriteAgentId) },
            )
            ActionSheetItem(
                text = stringResource(R.string.action_remove_favorite),
                icon = LettaIcons.FavoriteBorder,
                onClick = { showMenu = false; onClearFavorite() },
            )
        }
    } else {
        Card(
            onClick = onSetFavorite,
            modifier = modifier.fillMaxWidth().padding(horizontal = LettaSpacing.screenHorizontal),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    LettaIcons.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.screen_home_set_favorite_prompt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PinnedAgentCard(
    name: String,
    onClick: () -> Unit,
    onUnpin: () -> Unit,
    onConfigure: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showMenu = true
                },
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                LettaIcons.Agent,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.statValue,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.screen_home_pinned_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }

    ActionSheet(
        show = showMenu,
        onDismiss = { showMenu = false },
        title = name,
    ) {
        ActionSheetItem(
            text = stringResource(R.string.action_configure_agent),
            icon = LettaIcons.Edit,
            onClick = { showMenu = false; onConfigure() },
        )
        ActionSheetItem(
            text = stringResource(R.string.action_unpin_agent),
            icon = LettaIcons.PinOff,
            onClick = { showMenu = false; onUnpin() },
        )
    }
}

@Composable
private fun resolveContextualInfo(
    shortcut: DashboardShortcut,
    state: DashboardUiState,
): String? {
    return when (shortcut) {
        DashboardShortcut.AGENTS -> state.agentCount?.let {
            stringResource(R.string.widget_tile_count_format, it)
        }
        DashboardShortcut.CONVERSATIONS -> state.conversationCount?.let {
            stringResource(R.string.widget_tile_count_format, it)
        }
        DashboardShortcut.TOOLS -> state.toolCount?.let {
            stringResource(R.string.widget_tile_count_format, it)
        }
        DashboardShortcut.BLOCKS -> state.blockCount?.let {
            stringResource(R.string.widget_tile_count_format, it)
        }
        else -> {
            if (shortcut.descriptionResId != 0) {
                stringResource(shortcut.descriptionResId)
            } else null
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DashboardWidgetTile(
    shortcut: DashboardShortcut,
    contextualInfo: String?,
    onClick: () -> Unit,
    onUnpin: () -> Unit,
    isDragging: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    var showMenu by remember { mutableStateOf(false) }
    val accentColors = MaterialTheme.customColors

    val containerColor = when (shortcut.group) {
        DashboardShortcut.Group.PRIMARY -> accentColors.freshAccentContainer
        DashboardShortcut.Group.SECONDARY -> MaterialTheme.colorScheme.secondaryContainer
        DashboardShortcut.Group.UTILITY -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when (shortcut.group) {
        DashboardShortcut.Group.PRIMARY -> accentColors.freshAccent
        DashboardShortcut.Group.SECONDARY -> MaterialTheme.colorScheme.onSecondaryContainer
        DashboardShortcut.Group.UTILITY -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "tileScale",
    )
    val elevation by animateFloatAsState(
        targetValue = if (isDragging) 8f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "tileElevation",
    )

    Card(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = elevation * density
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showMenu = true
                },
            ),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = shortcut.icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (contextualInfo != null) {
                Text(
                    text = contextualInfo,
                    style = MaterialTheme.typography.statValue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = stringResource(shortcut.labelResId),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    ActionSheet(
        show = showMenu,
        onDismiss = { showMenu = false },
        title = stringResource(shortcut.labelResId),
    ) {
        ActionSheetItem(
            text = stringResource(R.string.action_unpin_shortcut),
            icon = LettaIcons.PinOff,
            onClick = { showMenu = false; onUnpin() },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReorderableWidgetGrid(
    shortcuts: ImmutableList<DashboardShortcut>,
    state: DashboardUiState,
    onShortcutClick: (DashboardShortcut) -> Unit,
    onUnpinShortcut: (DashboardShortcut) -> Unit,
    onReorder: (List<DashboardShortcut>) -> Unit,
    columns: Int,
    modifier: Modifier = Modifier,
) {
    var currentList by remember(shortcuts) { mutableStateOf(shortcuts.toList()) }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val itemRects = remember { mutableStateMapOf<Int, Rect>() }
    val haptic = LocalHapticFeedback.current

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                return if (draggingIndex != null) available else Offset.Zero
            }
        }
    }

    val gap = LettaSpacing.cardGap

    Layout(
        content = {
            currentList.forEachIndexed { index, shortcut ->
                key(shortcut) {
                    val isDragging = draggingIndex == index

                    // Track previous slot for easing animation
                    var previousSlot by remember { mutableIntStateOf(index) }
                    val slotOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }

                    LaunchedEffect(index) {
                        if (previousSlot != index && draggingIndex != index) {
                            // Item was displaced — animate from old position to new
                            val cols = columns
                            val oldCol = previousSlot % cols
                            val newCol = index % cols
                            val oldRow = previousSlot / cols
                            val newRow = index / cols

                            // We compute pixel delta using measured rects if available
                            val oldRect = itemRects[previousSlot]
                            val newRect = itemRects[index]
                            if (oldRect != null && newRect != null) {
                                val delta = Offset(
                                    oldRect.left - newRect.left,
                                    oldRect.top - newRect.top,
                                )
                                slotOffset.snapTo(delta)
                                slotOffset.animateTo(
                                    targetValue = Offset.Zero,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow,
                                    ),
                                )
                            }
                        }
                        previousSlot = index
                    }

                    Box(
                        modifier = Modifier
                            .then(if (isDragging) Modifier.zIndex(10f) else Modifier)
                            .then(
                                if (isDragging) {
                                    Modifier.offset {
                                        IntOffset(
                                            dragOffset.x.roundToInt(),
                                            dragOffset.y.roundToInt(),
                                        )
                                    }
                                } else {
                                    Modifier.offset {
                                        IntOffset(
                                            slotOffset.value.x.roundToInt(),
                                            slotOffset.value.y.roundToInt(),
                                        )
                                    }
                                },
                            )
                            .pointerInput(index) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        draggingIndex = index
                                        dragOffset = Offset.Zero
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffset += Offset(dragAmount.x, dragAmount.y)

                                        val draggedRect = itemRects[index] ?: return@detectDragGesturesAfterLongPress
                                        val draggedCenter = draggedRect.center + dragOffset
                                        val targetIndex = itemRects.entries
                                            .firstOrNull { (i, rect) ->
                                                i != index && rect.contains(draggedCenter)
                                            }?.key

                                        if (targetIndex != null && targetIndex != index) {
                                            val oldRect = itemRects[index]!!
                                            val newRect = itemRects[targetIndex]!!

                                            currentList = currentList.toMutableList().apply {
                                                val item = removeAt(index)
                                                add(targetIndex, item)
                                            }
                                            draggingIndex = targetIndex
                                            dragOffset += Offset(
                                                oldRect.left - newRect.left,
                                                oldRect.top - newRect.top,
                                            )
                                        }
                                    },
                                    onDragEnd = {
                                        draggingIndex = null
                                        dragOffset = Offset.Zero
                                        onReorder(currentList)
                                    },
                                    onDragCancel = {
                                        draggingIndex = null
                                        dragOffset = Offset.Zero
                                        currentList = shortcuts.toList()
                                    },
                                )
                            },
                    ) {
                        DashboardWidgetTile(
                            shortcut = shortcut,
                            contextualInfo = resolveContextualInfo(shortcut, state),
                            onClick = { onShortcutClick(shortcut) },
                            onUnpin = { onUnpinShortcut(shortcut) },
                            isDragging = isDragging,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .nestedScroll(nestedScrollConnection),
    ) { measurables, constraints ->
        val gapPx = gap.roundToPx()
        val totalGapWidth = gapPx * (columns - 1)
        val cellWidth = (constraints.maxWidth - totalGapWidth) / columns
        val cellConstraints = constraints.copy(
            minWidth = cellWidth,
            maxWidth = cellWidth,
            minHeight = 0,
        )

        val placeables = measurables.map { it.measure(cellConstraints) }
        val rows = placeables.chunked(columns)
        val rowHeights = rows.map { row -> row.maxOf { it.height } }
        val totalHeight = rowHeights.sum() + gapPx * (rowHeights.size - 1).coerceAtLeast(0)

        layout(constraints.maxWidth, totalHeight) {
            var y = 0
            rows.forEachIndexed { rowIndex, row ->
                var x = 0
                row.forEachIndexed { colIndex, placeable ->
                    val globalIndex = rowIndex * columns + colIndex
                    itemRects[globalIndex] = Rect(
                        Offset(x.toFloat(), y.toFloat()),
                        Size(cellWidth.toFloat(), rowHeights[rowIndex].toFloat()),
                    )
                    placeable.placeRelative(x, y)
                    x += cellWidth + gapPx
                }
                y += rowHeights[rowIndex] + gapPx
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SearchResultsContent(
    agentResults: List<Agent>,
    messageResults: List<ParsedSearchMessage>,
    toolResults: List<Tool>,
    blockResults: List<Block>,
    isSearching: Boolean,
    searchQuery: String,
    onAgentClick: (String) -> Unit,
    onMessageClick: (ParsedSearchMessage) -> Unit,
    onToolClick: (String) -> Unit,
    onBlockClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    val highlightTextColor = MaterialTheme.colorScheme.primary

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (agentResults.isNotEmpty()) {
            item(key = "agents-header") {
                Text(
                    text = stringResource(R.string.screen_home_search_agents_section),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            items(agentResults, key = { "agent-${it.id}" }) { agent ->
                Card(
                    onClick = { onAgentClick(agent.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            LettaIcons.Agent,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = highlightMatches(agent.name, searchQuery, highlightColor, highlightTextColor),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            agent.description?.let { desc ->
                                Text(
                                    text = highlightMatches(desc, searchQuery, highlightColor, highlightTextColor),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (toolResults.isNotEmpty()) {
            item(key = "tools-header") {
                Text(
                    text = stringResource(R.string.screen_home_search_tools_section),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
            }
            items(toolResults, key = { "tool-${it.id}" }) { tool ->
                Card(
                    onClick = { onToolClick(tool.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            LettaIcons.Tool,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = highlightMatches(tool.name, searchQuery, highlightColor, highlightTextColor),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            tool.description?.let { desc ->
                                Text(
                                    text = highlightMatches(desc, searchQuery, highlightColor, highlightTextColor),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (blockResults.isNotEmpty()) {
            item(key = "blocks-header") {
                Text(
                    text = stringResource(R.string.screen_home_search_blocks_section),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
            }
            items(blockResults, key = { "block-${it.id}" }) { block ->
                Card(
                    onClick = { onBlockClick(block.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            LettaIcons.ViewModule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = highlightMatches(block.label ?: "Unnamed", searchQuery, highlightColor, highlightTextColor),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            block.description?.let { desc ->
                                Text(
                                    text = highlightMatches(desc, searchQuery, highlightColor, highlightTextColor),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (messageResults.isNotEmpty()) {
            item(key = "messages-header") {
                Text(
                    text = stringResource(R.string.screen_home_search_messages_section),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
            }
            items(messageResults.size, key = { "msg-$it" }) { index ->
                val msg = messageResults[index]
                Card(
                    onClick = { onMessageClick(msg) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                LettaIcons.Chat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg.role?.replaceFirstChar { it.uppercase() } ?: "Message",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = highlightMatches(msg.content ?: "", searchQuery, highlightColor, highlightTextColor),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        if (isSearching) {
            item(key = "loading") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    LoadingIndicator()
                }
            }
        }

        if (!isSearching && agentResults.isEmpty() && messageResults.isEmpty() && toolResults.isEmpty() && blockResults.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.screen_home_search_no_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                )
            }
        }
    }
}

private fun highlightMatches(
    text: String,
    query: String,
    highlightColor: Color,
    matchTextColor: Color = Color.Unspecified,
) = buildAnnotatedString {
    if (query.isBlank()) {
        append(text)
        return@buildAnnotatedString
    }
    val lowerText = text.lowercase()
    val lowerQuery = query.trim().lowercase()
    var cursor = 0
    var matched = false
    while (cursor < text.length) {
        val matchIndex = lowerText.indexOf(lowerQuery, cursor)
        if (matchIndex < 0) {
            append(text.substring(cursor))
            break
        }
        matched = true
        append(text.substring(cursor, matchIndex))
        withStyle(
            SpanStyle(
                background = highlightColor,
                fontWeight = FontWeight.Bold,
                color = if (matchTextColor != Color.Unspecified) matchTextColor else Color.Unspecified,
            )
        ) {
            append(text.substring(matchIndex, matchIndex + lowerQuery.length))
        }
        cursor = matchIndex + lowerQuery.length
    }
}

private fun formatNumber(value: Int): String = String.format(Locale.US, "%,d", value)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatCard(
    label: String,
    value: String?,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColors = MaterialTheme.customColors
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = accentColors.freshAccentContainer,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColors.freshAccent,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.statValue,
                )
            } else {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.statValue,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
