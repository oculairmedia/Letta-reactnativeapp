package com.letta.mobile.ui.screens.tools

import com.letta.mobile.data.model.Tool
import com.letta.mobile.data.repository.McpServerRepository
import com.letta.mobile.data.repository.ToolRepository
import com.letta.mobile.testutil.FakeToolApi
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AllToolsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepository: FakeToolRepository
    private lateinit var mockMcpServerRepository: McpServerRepository
    private lateinit var viewModel: AllToolsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeToolRepository()
        mockMcpServerRepository = mockk(relaxed = true)
        viewModel = AllToolsViewModel(fakeRepository, mockMcpServerRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `updateSearchQuery filters tools by name and description`() = runTest {
        fakeRepository.setTools(
            listOf(
                Tool(id = "t1", name = "weather_lookup", description = "Forecast helper"),
                Tool(id = "t2", name = "calendar_sync", description = "Calendar helper"),
            )
        )
        viewModel.loadTools()

        viewModel.updateSearchQuery("forecast")

        val filtered = viewModel.getFilteredTools()
        assertEquals(1, filtered.size)
        assertEquals("weather_lookup", filtered.first().name)
    }

    @Test
    fun `getFilteredTools returns all tools when query blank`() = runTest {
        fakeRepository.setTools(
            listOf(
                Tool(id = "t1", name = "weather_lookup"),
                Tool(id = "t2", name = "calendar_sync"),
            )
        )
        viewModel.loadTools()

        val filtered = viewModel.getFilteredTools()
        assertEquals(2, filtered.size)
    }

    private class FakeToolRepository : ToolRepository(FakeToolApi()) {
        private val toolsFlow = MutableStateFlow<List<Tool>>(emptyList())

        fun setTools(tools: List<Tool>) {
            toolsFlow.value = tools
        }

        override fun getTools(): Flow<List<Tool>> = toolsFlow

        override suspend fun refreshTools() {
        }
    }
}
