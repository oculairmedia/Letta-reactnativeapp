package com.letta.mobile.ui.screens.blocks

import app.cash.turbine.test
import com.letta.mobile.data.model.Block
import com.letta.mobile.data.model.BlockCreateParams
import com.letta.mobile.data.model.BlockUpdateParams
import com.letta.mobile.data.repository.api.IBlockRepository
import com.letta.mobile.ui.common.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BlockLibraryViewModelTest {

    private lateinit var fakeRepo: FakeBlockRepo
    private lateinit var viewModel: BlockLibraryViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeBlockRepo()
        viewModel = BlockLibraryViewModel(fakeRepo)
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `loadBlocks sets Success with block list`() = runTest {
        fakeRepo.allBlocks = listOf(
            Block(id = "b1", label = "persona", value = "I am helpful."),
            Block(id = "b2", label = "human", value = "User info."),
        )
        viewModel.loadBlocks()
        viewModel.uiState.test {
            val state = awaitItem() as UiState.Success
            assertEquals(2, state.data.blocks.size)
        }
    }

    @Test
    fun `loadBlocks sets Error on failure`() = runTest {
        fakeRepo.shouldFail = true
        viewModel.loadBlocks()
        viewModel.uiState.test { assertTrue(awaitItem() is UiState.Error) }
    }

    @Test
    fun `setFilter updates filter and reloads`() = runTest {
        fakeRepo.allBlocks = listOf(
            Block(id = "b1", label = "persona", value = "Persona block."),
            Block(id = "b2", label = "human", value = "Human block."),
        )
        viewModel.setFilter(label = "persona", isTemplate = null)
        viewModel.uiState.test {
            val state = awaitItem() as UiState.Success
            assertEquals(1, state.data.blocks.size)
            assertEquals("persona", state.data.blocks[0].label)
            assertEquals("persona", state.data.filterLabel)
        }
    }

    @Test
    fun `loadBlocks with template filter returns only templates`() = runTest {
        fakeRepo.allBlocks = listOf(
            Block(id = "b1", label = "persona", value = "Template", isTemplate = true),
            Block(id = "b2", label = "human", value = "Not template", isTemplate = false),
        )
        viewModel.setFilter(label = null, isTemplate = true)
        viewModel.uiState.test {
            val state = awaitItem() as UiState.Success
            assertEquals(1, state.data.blocks.size)
            assertEquals(true, state.data.blocks[0].isTemplate)
            assertEquals(true, state.data.filterTemplate)
        }
    }

    @Test
    fun `updateSearchQuery filters loaded blocks locally`() = runTest {
        fakeRepo.allBlocks = listOf(
            Block(id = "b1", label = "persona", value = "Template block", description = "Behavior rules"),
            Block(id = "b2", label = "human", value = "Human block", description = "User profile"),
        )
        viewModel.loadBlocks()

        viewModel.updateSearchQuery("behavior")

        val filtered = viewModel.getFilteredBlocks()
        assertEquals(1, filtered.size)
        assertEquals("persona", filtered.first().label)
    }

    @Test
    fun `deleteBlock delegates to repository`() = runTest {
        fakeRepo.allBlocks = listOf(Block(id = "b1", label = "persona", value = "Template block"))

        viewModel.deleteBlock("b1")

        assertEquals(listOf("b1"), fakeRepo.deletedBlockIds)
    }

    private class FakeBlockRepo : IBlockRepository {
        var allBlocks = listOf<Block>()
        var shouldFail = false
        val deletedBlockIds = mutableListOf<String>()

        override suspend fun listAllBlocks(label: String?, isTemplate: Boolean?): List<Block> {
            if (shouldFail) throw Exception("Failed to load blocks")
            return allBlocks.filter { block ->
                (label == null || block.label == label) &&
                (isTemplate == null || block.isTemplate == isTemplate)
            }
        }

        override suspend fun getBlocks(agentId: String): List<Block> = emptyList()
        override suspend fun updateBlock(agentId: String, blockLabel: String, params: BlockUpdateParams): Block =
            Block(id = "stub", label = blockLabel, value = params.value ?: "")
        override suspend fun createBlock(params: BlockCreateParams): Block =
            Block(id = "stub", label = params.label, value = params.value)
        override suspend fun deleteBlock(blockId: String) {
            deletedBlockIds.add(blockId)
            allBlocks = allBlocks.filterNot { it.id == blockId }
        }
        override suspend fun attachBlock(agentId: String, blockId: String): Block =
            Block(id = blockId, label = "stub", value = "")
        override suspend fun detachBlock(agentId: String, blockId: String) {}
    }
}
