package com.letta.mobile.ui.screens.tools

import com.letta.mobile.data.model.Tool
import com.letta.mobile.data.repository.McpServerRepository
import com.letta.mobile.data.repository.ToolRepository
import com.letta.mobile.testutil.FakeToolApi
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
        coEvery { mockMcpServerRepository.fetchAllMcpTools() } returns emptyList()
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
        awaitSuccessState()

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
        awaitSuccessState()

        val filtered = viewModel.getFilteredTools()
        assertEquals(2, filtered.size)
    }

    private class FakeToolRepository : ToolRepository(FakeToolApi()) {
        private var tools: List<Tool> = emptyList()

        fun setTools(tools: List<Tool>) {
            this.tools = tools
        }

        override suspend fun fetchToolsPage(limit: Int, offset: Int): List<Tool> =
            tools.drop(offset).take(limit)
    }

    private suspend fun awaitSuccessState(): AllToolsUiState {
        return viewModel.uiState.first { it is com.letta.mobile.ui.common.UiState.Success }
            .let { (it as com.letta.mobile.ui.common.UiState.Success).data }
    }
}
