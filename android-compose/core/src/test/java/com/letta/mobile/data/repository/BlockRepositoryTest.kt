package com.letta.mobile.data.repository

import com.letta.mobile.data.model.Block
import com.letta.mobile.data.model.BlockUpdateParams
import com.letta.mobile.testutil.FakeBlockApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BlockRepositoryTest {

    private lateinit var fakeApi: FakeBlockApi
    private lateinit var repository: BlockRepository

    @Before
    fun setup() {
        fakeApi = FakeBlockApi()
        repository = BlockRepository(fakeApi)
    }

    @Test
    fun `updateBlock calls API with correct params`() = runTest {
        repository.updateBlock(
            "a1",
            "persona",
            BlockUpdateParams(value = "New persona value", description = "desc", limit = 256)
        )
        assertTrue(fakeApi.calls.contains("updateBlock:a1:persona"))
        assertEquals("desc", fakeApi.lastUpdateParams?.description)
        assertEquals(256, fakeApi.lastUpdateParams?.limit)
    }

    @Test
    fun `updateBlock returns updated block`() = runTest {
        val result = repository.updateBlock("a1", "human", BlockUpdateParams(value = "Updated human block"))
        assertEquals("human", result.label)
        assertEquals("Updated human block", result.value)
    }

    @Test(expected = com.letta.mobile.data.api.ApiException::class)
    fun `updateBlock throws on API failure`() = runTest {
        fakeApi.shouldFail = true
        repository.updateBlock("a1", "persona", BlockUpdateParams(value = "value"))
    }

    @Test
    fun `listAllBlocks returns all blocks`() = runTest {
        fakeApi.allBlocks.add(Block(id = "b1", label = "persona", value = "Test persona"))
        fakeApi.allBlocks.add(Block(id = "b2", label = "human", value = "Test human"))
        val result = repository.listAllBlocks()
        assertEquals(2, result.size)
        assertTrue(fakeApi.calls.contains("listAllBlocks"))
    }

    @Test
    fun `listAllBlocks filters by label`() = runTest {
        fakeApi.allBlocks.add(Block(id = "b1", label = "persona", value = "Test persona"))
        fakeApi.allBlocks.add(Block(id = "b2", label = "human", value = "Test human"))
        val result = repository.listAllBlocks(label = "persona")
        assertEquals(1, result.size)
        assertEquals("persona", result[0].label)
    }

    @Test
    fun `listAllBlocks filters by isTemplate`() = runTest {
        fakeApi.allBlocks.add(Block(id = "b1", label = "persona", value = "Template", isTemplate = true))
        fakeApi.allBlocks.add(Block(id = "b2", label = "human", value = "Not template", isTemplate = false))
        val result = repository.listAllBlocks(isTemplate = true)
        assertEquals(1, result.size)
        assertEquals(true, result[0].isTemplate)
    }

    @Test(expected = com.letta.mobile.data.api.ApiException::class)
    fun `listAllBlocks throws on API failure`() = runTest {
        fakeApi.shouldFail = true
        repository.listAllBlocks()
    }
}
