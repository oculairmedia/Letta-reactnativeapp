package com.letta.mobile.ui.screens.projects

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.letta.mobile.data.model.ProjectIssueDetail
import com.letta.mobile.data.model.ProjectIssueListParams
import com.letta.mobile.data.model.ProjectIssueSummary
import com.letta.mobile.data.repository.ProjectWorkRepository
import com.letta.mobile.ui.common.UiState
import com.letta.mobile.ui.navigation.ProjectIssueDetailRoute
import com.letta.mobile.ui.navigation.ProjectIssuesRoute
import com.letta.mobile.util.mapErrorToUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@androidx.compose.runtime.Immutable
data class ProjectIssuesUiState(
    val projectId: String,
    val projectName: String?,
    val readyWork: ImmutableList<ProjectIssueSummary> = persistentListOf(),
    val issues: ImmutableList<ProjectIssueSummary> = persistentListOf(),
    val completedTimeline: ImmutableList<ProjectIssueTimelineItem> = persistentListOf(),
    val creationBuckets: ImmutableList<ProjectIssueCreationBucket> = persistentListOf(),
    val searchQuery: String = "",
    val selectedStatus: String? = null,
    val isRefreshing: Boolean = false,
)

@androidx.compose.runtime.Immutable
data class ProjectIssueTimelineItem(
    val id: String,
    val title: String,
    val completedAt: String,
    val statusLabel: String,
    val priority: String?,
    val type: String?,
)

@androidx.compose.runtime.Immutable
data class ProjectIssueCreationBucket(
    val date: String,
    val label: String,
    val count: Int,
)

private data class ProjectIssueAnalytics(
    val completedTimeline: ImmutableList<ProjectIssueTimelineItem>,
    val creationBuckets: ImmutableList<ProjectIssueCreationBucket>,
)

@androidx.compose.runtime.Immutable
data class ProjectIssueDetailUiState(
    val projectId: String,
    val projectName: String?,
    val issueId: String,
    val issue: ProjectIssueDetail,
    val showActions: Boolean = false,
    val showNoteDialog: Boolean = false,
    val isMutating: Boolean = false,
)

sealed interface ProjectIssuesUiEvent {
    data class ShowMessage(val message: String) : ProjectIssuesUiEvent
}

@HiltViewModel
class ProjectIssuesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val projectWorkRepository: ProjectWorkRepository,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<ProjectIssuesRoute>()

    private val _uiState = MutableStateFlow<UiState<ProjectIssuesUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<ProjectIssuesUiState>> = _uiState.asStateFlow()

    private val _events = Channel<ProjectIssuesUiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadIssues()
    }

    fun refresh() = loadIssues(forceRefresh = true)

    fun updateSearchQuery(query: String) {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        _uiState.value = UiState.Success(current.copy(searchQuery = query))
    }

    fun selectStatus(status: String?) {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        _uiState.value = UiState.Success(current.copy(selectedStatus = status))
    }

    fun filteredIssues(): List<ProjectIssueSummary> {
        val current = (_uiState.value as? UiState.Success)?.data ?: return emptyList()
        val query = current.searchQuery.trim().lowercase()
        return current.issues.filter { issue ->
            val matchesStatus = current.selectedStatus == null || issue.status.equals(current.selectedStatus, ignoreCase = true)
            val matchesQuery = query.isBlank() ||
                issue.id.lowercase().contains(query) ||
                issue.title.lowercase().contains(query) ||
                issue.summary?.lowercase()?.contains(query) == true ||
                issue.labels.any { it.lowercase().contains(query) }
            matchesStatus && matchesQuery
        }
    }

    private fun loadIssues(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val current = (_uiState.value as? UiState.Success)?.data
            if (current == null) {
                _uiState.value = UiState.Loading
            } else {
                _uiState.value = UiState.Success(current.copy(isRefreshing = true))
            }

            val readyResult = runCatching {
                projectWorkRepository.refreshReadyWork(route.projectId, limit = 50)
            }
            val issuesResult = runCatching {
                projectWorkRepository.refreshIssues(
                    projectId = route.projectId,
                    params = ProjectIssueListParams(limit = 100, sort = "updated_desc"),
                )
            }

            issuesResult.onSuccess { issues ->
                val ready = readyResult.getOrElse { emptyList() }
                val analytics = withContext(Dispatchers.Default) {
                    ProjectIssueAnalytics(
                        completedTimeline = buildCompletedIssueTimeline(issues).toImmutableList(),
                        creationBuckets = buildIssueCreationBuckets(issues).toImmutableList(),
                    )
                }
                _uiState.value = UiState.Success(
                    ProjectIssuesUiState(
                        projectId = route.projectId,
                        projectName = route.projectName,
                        readyWork = ready.toImmutableList(),
                        issues = issues.toImmutableList(),
                        completedTimeline = analytics.completedTimeline,
                        creationBuckets = analytics.creationBuckets,
                        searchQuery = current?.searchQuery.orEmpty(),
                        selectedStatus = current?.selectedStatus,
                        isRefreshing = false,
                    )
                )
                readyResult.exceptionOrNull()?.let { error ->
                    _events.trySend(ProjectIssuesUiEvent.ShowMessage(mapErrorToUserMessage(error.toException(), "Ready work is unavailable")))
                }
            }.onFailure { error ->
                val message = mapErrorToUserMessage(error.toException(), "Failed to load project issues")
                if (current == null || forceRefresh.not()) {
                    _uiState.value = UiState.Error(message)
                } else {
                    _uiState.value = UiState.Success(current.copy(isRefreshing = false))
                    _events.trySend(ProjectIssuesUiEvent.ShowMessage(message))
                }
            }
        }
    }
}

@HiltViewModel
class ProjectIssueDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val projectWorkRepository: ProjectWorkRepository,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<ProjectIssueDetailRoute>()

    private val _uiState = MutableStateFlow<UiState<ProjectIssueDetailUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<ProjectIssueDetailUiState>> = _uiState.asStateFlow()

    private val _events = Channel<ProjectIssuesUiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadIssue()
    }

    fun refresh() = loadIssue(forceRefresh = true)

    fun showActions(show: Boolean) = updateSuccess { it.copy(showActions = show) }

    fun showNoteDialog(show: Boolean) = updateSuccess { it.copy(showNoteDialog = show, showActions = false) }

    fun claimIssue() = mutate("Claimed issue") { issue ->
        projectWorkRepository.claimIssue(issue.id, assignee = "Android", ifMatch = requireEtag(issue))
    }

    fun unclaimIssue() = mutate("Cleared assignee") { issue ->
        projectWorkRepository.unclaimIssue(issue.id, ifMatch = requireEtag(issue))
    }

    fun closeIssue() = mutate("Closed issue") { issue ->
        projectWorkRepository.closeIssue(issue.id, reason = "Closed from Letta Mobile", ifMatch = requireEtag(issue))
    }

    fun reopenIssue() = mutate("Reopened issue") { issue ->
        projectWorkRepository.reopenIssue(issue.id, reason = "Reopened from Letta Mobile", ifMatch = requireEtag(issue))
    }

    fun addNote(note: String) = mutate("Added note") { issue ->
        projectWorkRepository.addIssueNote(issue.id, note = note.trim(), ifMatch = requireEtag(issue))
    }

    private fun loadIssue(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if ((_uiState.value as? UiState.Success) == null) _uiState.value = UiState.Loading
            runCatching {
                projectWorkRepository.getIssue(route.issueId, forceRefresh = forceRefresh)
            }.onSuccess { issue ->
                _uiState.value = UiState.Success(
                    ProjectIssueDetailUiState(
                        projectId = route.projectId,
                        projectName = route.projectName,
                        issueId = route.issueId,
                        issue = issue,
                    )
                )
            }.onFailure { error ->
                _uiState.value = UiState.Error(mapErrorToUserMessage(error.toException(), "Failed to load issue"))
            }
        }
    }

    private fun mutate(
        successMessage: String,
        action: suspend (ProjectIssueDetail) -> ProjectIssueSummary,
    ) {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            _uiState.value = UiState.Success(current.copy(isMutating = true, showActions = false, showNoteDialog = false))
            runCatching {
                action(current.issue)
                projectWorkRepository.getIssue(current.issue.id, forceRefresh = true)
            }.onSuccess { issue ->
                _uiState.value = UiState.Success(current.copy(issue = issue, isMutating = false))
                _events.trySend(ProjectIssuesUiEvent.ShowMessage(successMessage))
            }.onFailure { error ->
                _uiState.value = UiState.Success(current.copy(isMutating = false))
                _events.trySend(ProjectIssuesUiEvent.ShowMessage(mapErrorToUserMessage(error.toException(), "Issue action failed")))
            }
        }
    }

    private fun requireEtag(issue: ProjectIssueDetail): String =
        issue.etag ?: throw IllegalStateException("Refresh this issue before changing it; the server did not provide an ETag.")

    private fun updateSuccess(transform: (ProjectIssueDetailUiState) -> ProjectIssueDetailUiState) {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        _uiState.value = UiState.Success(transform(current))
    }

}

private fun Throwable.toException(): Exception = this as? Exception ?: Exception(this)

internal fun buildCompletedIssueTimeline(
    issues: List<ProjectIssueSummary>,
    limit: Int = 30,
    zoneId: ZoneId = ZoneId.systemDefault(),
): List<ProjectIssueTimelineItem> = issues
    .asSequence()
    .filter(ProjectIssueSummary::isCompletedIssue)
    .mapNotNull { issue ->
        val completedAt = listOfNotNull(issue.updatedAt, issue.createdAt)
            .firstNotNullOfOrNull { timestamp ->
                timestamp.toIssueInstantOrNull(zoneId)?.let { instant -> timestamp to instant }
            }
            ?: return@mapNotNull null
        completedAt.second to ProjectIssueTimelineItem(
            id = issue.id,
            title = issue.title,
            completedAt = completedAt.first,
            statusLabel = issue.statusLabel ?: issue.status,
            priority = issue.priority,
            type = issue.type,
        )
    }
    .sortedByDescending { it.first }
    .take(limit)
    .map { it.second }
    .toList()

internal fun buildIssueCreationBuckets(
    issues: List<ProjectIssueSummary>,
    maxBuckets: Int = 12,
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
): List<ProjectIssueCreationBucket> {
    val groupedByDate = issues
        .mapNotNull { issue -> issue.createdAt?.toIssueLocalDateOrNull(zoneId) }
        .groupingBy { it }
        .eachCount()

    val sameYearFormatter = DateTimeFormatter.ofPattern("MMM d", locale)
    val withYearFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", locale)
    val currentYear = LocalDate.now(zoneId).year

    return groupedByDate
        .toSortedMap()
        .entries
        .toList()
        .takeLast(maxBuckets)
        .map { (date, count) ->
            ProjectIssueCreationBucket(
                date = date.toString(),
                label = date.format(if (date.year == currentYear) sameYearFormatter else withYearFormatter),
                count = count,
            )
        }
}

private fun ProjectIssueSummary.isCompletedIssue(): Boolean {
    val normalizedStatus = status.lowercase(Locale.ROOT)
    val normalizedLabel = statusLabel?.lowercase(Locale.ROOT).orEmpty()
    return normalizedStatus in completedStatusValues ||
        completedStatusValues.any { completedStatus -> normalizedLabel.contains(completedStatus) }
}

private val completedStatusValues = setOf("closed", "done", "completed", "complete", "resolved")

private fun String.toIssueLocalDateOrNull(zoneId: ZoneId): LocalDate? =
    toIssueInstantOrNull(zoneId)?.atZone(zoneId)?.toLocalDate()
        ?: runCatching { LocalDate.parse(this) }.getOrNull()

private fun String.toIssueInstantOrNull(zoneId: ZoneId): Instant? =
    runCatching { Instant.parse(this) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(this).toInstant() }.getOrNull()
        ?: runCatching { LocalDateTime.parse(this).atZone(zoneId).toInstant() }.getOrNull()
        ?: runCatching { LocalDate.parse(this).atStartOfDay(zoneId).toInstant() }.getOrNull()
