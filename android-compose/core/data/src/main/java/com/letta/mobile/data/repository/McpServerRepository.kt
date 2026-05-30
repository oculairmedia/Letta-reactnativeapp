package com.letta.mobile.data.repository

import com.letta.mobile.data.api.McpServerApi
import com.letta.mobile.data.model.McpServer
import com.letta.mobile.data.model.McpServerCreateParams
import com.letta.mobile.data.model.McpServerResyncResult
import com.letta.mobile.data.model.McpServerUpdateParams
import com.letta.mobile.data.model.McpToolExecuteParams
import com.letta.mobile.data.model.McpToolExecutionResult
import com.letta.mobile.data.model.Tool
import com.letta.mobile.data.repository.api.IMcpServerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject

open class McpServerRepository @Inject constructor(
    private val mcpServerApi: McpServerApi,
) : IMcpServerRepository {
    private val _servers = MutableStateFlow<List<McpServer>>(emptyList())
    override val servers: StateFlow<List<McpServer>> = _servers.asStateFlow()

    private val _toolsByServer = MutableStateFlow<Map<String, List<Tool>>>(emptyMap())

    override open fun getServers(): Flow<List<McpServer>> = servers

    override open fun getServerTools(serverId: String): Flow<List<Tool>> {
        return _toolsByServer.map { it[serverId] ?: emptyList() }
    }

    override open suspend fun refreshServers() {
        _servers.update { mcpServerApi.listMcpServers() }
    }

    override open suspend fun refreshServerTools(serverId: String) {
        val tools = mcpServerApi.listMcpServerTools(serverId)
        _toolsByServer.update { current -> current.toMutableMap().apply {
                    put(serverId, tools)
                } }
    }

    override open suspend fun resyncServerTools(serverId: String): McpServerResyncResult {
        val result = mcpServerApi.refreshMcpServerTools(serverId)
        refreshServerTools(serverId)
        return result
    }

    override open suspend fun runServerTool(
        serverId: String,
        toolId: String,
        params: McpToolExecuteParams,
    ): McpToolExecutionResult {
        return mcpServerApi.runMcpServerTool(serverId, toolId, params)
    }

    override open suspend fun fetchAllMcpTools(): List<Tool> {
        refreshServers()
        return _servers.value.flatMap { server ->
            try {
                mcpServerApi.listMcpServerTools(server.id)
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    override open suspend fun createServer(params: McpServerCreateParams): McpServer {
        val server = mcpServerApi.createMcpServer(params)
        refreshServers()
        return server
    }

    override open suspend fun updateServer(id: String, params: McpServerUpdateParams): McpServer {
        val server = mcpServerApi.updateMcpServer(id, params)
        refreshServers()
        return server
    }

    override open suspend fun deleteServer(id: String) {
        mcpServerApi.deleteMcpServer(id)
        refreshServers()
        _toolsByServer.update { current -> current.toMutableMap().apply {
                    remove(id)
                } }
    }
}
