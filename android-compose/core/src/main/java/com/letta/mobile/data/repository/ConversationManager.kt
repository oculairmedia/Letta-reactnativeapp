package com.letta.mobile.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationManager @Inject constructor(
    private val conversationRepository: ConversationRepository,
) {
    private val activeConversationIds = MutableStateFlow<Map<String, String>>(emptyMap())

    fun activeConversationIds(): StateFlow<Map<String, String>> = activeConversationIds.asStateFlow()

    fun getActiveConversationId(agentId: String): String? = activeConversationIds.value[agentId]

    fun observeActiveConversationId(agentId: String): Flow<String?> {
        return activeConversationIds
            .map { conversations -> conversations[agentId] }
            .distinctUntilChanged()
    }

    fun setActiveConversation(agentId: String, conversationId: String) {
        require(conversationId.isNotBlank()) { "conversationId cannot be blank" }
        activeConversationIds.value = activeConversationIds.value.toMutableMap().apply {
            put(agentId, conversationId)
        }
    }

    fun clearActiveConversation(agentId: String) {
        activeConversationIds.value = activeConversationIds.value.toMutableMap().apply {
            remove(agentId)
        }
    }

    suspend fun resolveAndSetActiveConversation(
        agentId: String,
        maxAgeMs: Long = DEFAULT_CACHE_TTL_MS,
    ): String? {
        conversationRepository.refreshConversationsIfStale(agentId, maxAgeMs)
        val mostRecent = conversationRepository.getCachedConversations(agentId)
            .sortedByDescending { it.lastMessageAt ?: it.createdAt ?: "" }
            .firstOrNull()

        return mostRecent?.id?.also { conversationId ->
            setActiveConversation(agentId, conversationId)
        }
    }

    suspend fun createAndSetActiveConversation(agentId: String, summary: String? = null): String {
        val conversation = conversationRepository.createConversation(agentId, summary)
        setActiveConversation(agentId, conversation.id)
        return conversation.id
    }

    companion object {
        private const val DEFAULT_CACHE_TTL_MS = 30_000L
    }
}
