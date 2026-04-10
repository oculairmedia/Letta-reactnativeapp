package com.letta.mobile.data.repository.api

import com.letta.mobile.data.model.Block
import com.letta.mobile.data.model.BlockCreateParams
import com.letta.mobile.data.model.BlockUpdateParams

interface IBlockRepository {
    suspend fun getBlocks(agentId: String): List<Block>
    suspend fun updateAgentBlock(agentId: String, blockLabel: String, params: BlockUpdateParams): Block
    suspend fun updateGlobalBlock(
        blockId: String,
        params: BlockUpdateParams,
        clearDescription: Boolean = false,
        clearLimit: Boolean = false,
    ): Block
    suspend fun createBlock(params: BlockCreateParams): Block
    suspend fun deleteBlock(blockId: String)
    suspend fun attachBlock(agentId: String, blockId: String)
    suspend fun detachBlock(agentId: String, blockId: String)
    suspend fun listAllBlocks(label: String? = null, isTemplate: Boolean? = null): List<Block>
}
