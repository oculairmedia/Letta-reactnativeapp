package com.letta.mobile.data.repository.api

import com.letta.mobile.data.model.AgentId
import com.letta.mobile.data.model.Conversation
import com.letta.mobile.data.model.ConversationId
import kotlinx.coroutines.flow.Flow

interface IConversationRepository {
    fun getConversations(agentId: AgentId): Flow<List<Conversation>>
    fun getCachedConversations(agentId: AgentId): List<Conversation>
    fun hasFreshConversations(agentId: AgentId, maxAgeMs: Long): Boolean
    suspend fun refreshConversations(agentId: AgentId)
    suspend fun refreshConversationsIfStale(agentId: AgentId, maxAgeMs: Long): Boolean
    suspend fun getConversation(id: ConversationId): Conversation
    suspend fun createConversation(agentId: AgentId, summary: String? = null): Conversation
    suspend fun deleteConversation(id: ConversationId, agentId: AgentId)
    suspend fun updateConversation(id: ConversationId, agentId: AgentId, summary: String)
    suspend fun setConversationArchived(id: ConversationId, agentId: AgentId, archived: Boolean)
    suspend fun cancelConversation(id: ConversationId, agentId: AgentId? = null)
    suspend fun recompileConversation(id: ConversationId, dryRun: Boolean = false, agentId: AgentId? = null): String
    suspend fun forkConversation(id: ConversationId, agentId: AgentId): Conversation
}
