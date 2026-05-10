package com.letta.mobile.data.repository

import com.letta.mobile.data.api.ConversationApi
import com.letta.mobile.data.model.Conversation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

data class ConversationCountEstimate(
    val count: Int,
    val isApproximate: Boolean,
)

@Singleton
class AllConversationsRepository @Inject constructor(
    private val conversationApi: ConversationApi,
) {
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private val refreshMutex = Mutex()
    private var currentCursor: String? = null
    private var lastRefreshAtMillis: Long = 0L
    private var hasLoadedAtLeastOnce: Boolean = false

    suspend fun loadNextPage() {
        if (!_hasMore.value) return

        val newConversations = conversationApi.listConversations(
            limit = PAGE_SIZE,
            after = currentCursor
        )

        hasLoadedAtLeastOnce = true
        if (newConversations.isEmpty() || newConversations.size < PAGE_SIZE) {
            _hasMore.update { false }
        }

        if (newConversations.isNotEmpty()) {
            _conversations.update { current ->
                val existingIds = current.map { it.id }.toSet()
                val deduped = newConversations.filter { it.id !in existingIds }
                current + deduped
            }
            currentCursor = newConversations.last().id
        }
    }

    suspend fun refresh() = refreshMutex.withLock {
        refreshLocked()
    }

    private suspend fun refreshLocked() {
        currentCursor = null
        hasLoadedAtLeastOnce = false
        _conversations.update { emptyList() }
        _hasMore.update { true }
        loadNextPage()
        lastRefreshAtMillis = System.currentTimeMillis()
    }

    fun hasFreshConversations(maxAgeMs: Long): Boolean {
        return hasLoadedAtLeastOnce && System.currentTimeMillis() - lastRefreshAtMillis <= maxAgeMs
    }

    suspend fun refreshIfStale(maxAgeMs: Long): Boolean = refreshMutex.withLock {
        if (hasFreshConversations(maxAgeMs)) return@withLock false
        refreshLocked()
        true
    }

    fun handleOptimisticUpdate(conversation: Conversation) {
        _conversations.update { current ->
            val index = current.indexOfFirst { it.id == conversation.id }
            if (index >= 0) {
                current.toMutableList().apply { this[index] = conversation }
            } else {
                listOf(conversation) + current
            }
        }
    }

    fun handleOptimisticDelete(conversationId: String) {
        _conversations.update { current -> current.filter { it.id != conversationId } }
    }

    /**
     * Lightweight loaded-page count estimate.
     *
     * Letta API doesn't have a /v1/conversations/count endpoint. Do not make a
     * dedicated network request just to compute a dashboard count; refresh or
     * page the list first, then display [ConversationCountEstimate.count] as an
     * exact count when [ConversationCountEstimate.isApproximate] is false or as a
     * lower bound (for example, "50+") when more pages are available.
     */
    fun loadedCountEstimate(): ConversationCountEstimate? {
        if (!hasLoadedAtLeastOnce && _conversations.value.isEmpty()) return null
        return ConversationCountEstimate(
            count = _conversations.value.size,
            isApproximate = _hasMore.value,
        )
    }

    /**
     * Legacy compatibility shim. Prefer [loadedCountEstimate] so callers must
     * decide how to display approximate/unknown counts. This method intentionally
     * performs no network I/O.
     */
    @Deprecated("Use loadedCountEstimate() and render approximate/unknown states explicitly.")
    suspend fun countConversations(): Int {
        return loadedCountEstimate()?.count ?: 0
    }

    companion object {
        private const val PAGE_SIZE = 50
    }
}
