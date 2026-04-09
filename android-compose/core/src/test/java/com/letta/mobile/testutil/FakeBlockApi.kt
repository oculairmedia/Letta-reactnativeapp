package com.letta.mobile.testutil

import com.letta.mobile.data.api.ApiException
import com.letta.mobile.data.api.BlockApi
import com.letta.mobile.data.model.Block
import com.letta.mobile.data.model.BlockUpdateParams
import io.mockk.mockk

class FakeBlockApi : BlockApi(mockk(relaxed = true)) {
    var blocks = mutableMapOf<String, MutableList<Block>>()
    var allBlocks = mutableListOf<Block>()
    var shouldFail = false
    val calls = mutableListOf<String>()
    var lastUpdateParams: BlockUpdateParams? = null

    override suspend fun updateAgentBlock(agentId: String, blockLabel: String, params: BlockUpdateParams): Block {
        calls.add("updateAgentBlock:$agentId:$blockLabel")
        lastUpdateParams = params
        if (shouldFail) throw ApiException(500, "Server error")
        val agentBlocks = blocks.getOrPut(agentId) { mutableListOf() }
        val index = agentBlocks.indexOfFirst { it.label == blockLabel }
        val existing = agentBlocks.getOrNull(index)
        val updated = Block(
            id = existing?.id ?: "block-${blockLabel}",
            label = blockLabel,
            value = params.value ?: existing?.value ?: "",
            description = params.description ?: existing?.description,
            limit = params.limit ?: existing?.limit,
        )
        if (index >= 0) {
            agentBlocks[index] = updated
        } else {
            agentBlocks.add(updated)
        }
        return updated
    }

    override suspend fun listAllBlocks(
        label: String?,
        isTemplate: Boolean?,
        limit: Int?,
        offset: Int?,
    ): List<Block> {
        calls.add("listAllBlocks")
        if (shouldFail) throw ApiException(500, "Server error")
        return allBlocks.filter { block ->
            (label == null || block.label == label) &&
            (isTemplate == null || block.isTemplate == isTemplate)
        }
    }
}
