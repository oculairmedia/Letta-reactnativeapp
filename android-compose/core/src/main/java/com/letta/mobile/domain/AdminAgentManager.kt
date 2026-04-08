package com.letta.mobile.domain

import com.letta.mobile.data.api.AgentApi
import com.letta.mobile.data.model.Agent
import com.letta.mobile.data.model.AgentCreateParams
import com.letta.mobile.data.model.BlockCreateParams
import com.letta.mobile.data.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminAgentManager @Inject constructor(
    private val agentApi: AgentApi,
    private val settingsRepository: SettingsRepository,
) {
    companion object {
        const val ADMIN_TAG = "letta-mobile-admin"
        private const val ADMIN_NAME = "Letta Mobile Admin"
        private const val ADMIN_DESCRIPTION = "Server administration assistant for Letta Mobile"
        private val ADMIN_SYSTEM_PROMPT = """
            You are the Letta Mobile server administrator agent. You help the user manage their Letta server from their mobile device.
            
            Your capabilities:
            - Report on server status and health
            - List and describe available agents
            - Help create and configure new agents
            - Explain agent capabilities, tools, and memory
            - Assist with troubleshooting
            
            Keep responses concise and mobile-friendly. Use bullet points for lists.
            When asked about server status, report what you know about the agents and tools available.
        """.trimIndent()
    }

    suspend fun ensureAdminAgent(): Agent {
        val existingId = settingsRepository.adminAgentId.value
        if (existingId != null) {
            try {
                return agentApi.getAgent(existingId)
            } catch (_: Exception) {
                settingsRepository.setAdminAgentId(null)
            }
        }

        val agents = agentApi.listAgents(tags = listOf(ADMIN_TAG))
        val existing = agents.firstOrNull()
        if (existing != null) {
            settingsRepository.setAdminAgentId(existing.id)
            return existing
        }

        val created = agentApi.createAgent(
            AgentCreateParams(
                name = ADMIN_NAME,
                description = ADMIN_DESCRIPTION,
                model = "anthropic-claude-max/opus-4-6-reasoning-medium",
                embedding = "dengcao/Qwen3-Embedding-4B:Q4_K_M",
                system = ADMIN_SYSTEM_PROMPT,
                tags = listOf(ADMIN_TAG),
                includeBaseTools = true,
                enableSleeptime = true,
                memoryBlocks = listOf(
                    BlockCreateParams(label = "persona", value = "I am a Letta server admin assistant."),
                    BlockCreateParams(label = "human", value = "The user is a Letta Mobile app user managing their server."),
                ),
            )
        )
        )
        settingsRepository.setAdminAgentId(created.id)
        return created
    }
}
