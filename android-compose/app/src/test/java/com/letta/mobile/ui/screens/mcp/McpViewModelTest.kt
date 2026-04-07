package com.letta.mobile.ui.screens.mcp

import app.cash.turbine.test
import com.letta.mobile.data.model.McpServer
import com.letta.mobile.data.model.McpServerCreateParams
import com.letta.mobile.data.model.Tool
import com.letta.mobile.data.repository.McpServerRepository
import com.letta.mobile.data.repository.ToolRepository
import com.letta.mobile.testutil.FakeMcpServerApi
import com.letta.mobile.testutil.FakeToolApi
import com.letta.mobile.testutil.TestData
import com.letta.mobile.ui.common.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
class McpViewModelTest {

    private lateinit var fakeMcpRepo: FakeMcpRepo
    private lateinit var fakeToolRepo: FakeToolRepo
    private lateinit var viewModel: McpViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeMcpRepo = FakeMcpRepo()
        fakeToolRepo = FakeToolRepo()
        viewModel = McpViewModel(fakeMcpRepo, fakeToolRepo)
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `loadData sets Success with servers and tools`() = runTest {
        fakeMcpRepo.setServers(listOf(TestData.mcpServer(id = "1")))
        fakeToolRepo.setTools(listOf(TestData.tool(id = "t1")))
        viewModel.loadData()
        viewModel.uiState.test {
            val state = awaitItem() as UiState.Success
            assertEquals(1, state.data.servers.size)
        }
    }

    @Test
    fun `loadData sets Error on failure`() = runTest {
        fakeMcpRepo.shouldFail = true
        viewModel.loadData()
        viewModel.uiState.test { assertTrue(awaitItem() is UiState.Error) }
    }

    @Test
    fun `selectTab updates selectedTab`() = runTest {
        fakeMcpRepo.setServers(emptyList())
        viewModel.loadData()
        viewModel.selectTab(1)
        viewModel.uiState.test {
            assertEquals(1, (awaitItem() as UiState.Success).data.selectedTab)
        }
    }

    @Test
    fun `deleteServer calls repo`() = runTest {
        fakeMcpRepo.setServers(listOf(TestData.mcpServer(id = "s1")))
        viewModel.loadData()
        viewModel.deleteServer("s1")
        assertTrue(fakeMcpRepo.deleteCalls.contains("s1"))
    }

    @Test
    fun `addServer calls repo`() = runTest {
        viewModel.addServer("New", "http://localhost")
        assertTrue(fakeMcpRepo.createCalls.isNotEmpty())
    }

    private class FakeMcpRepo : McpServerRepository(FakeMcpServerApi()) {
        private val _servers = MutableStateFlow<List<McpServer>>(emptyList())
        override val servers: StateFlow<List<McpServer>> = _servers.asStateFlow()
        var shouldFail = false
        val deleteCalls = mutableListOf<String>()
        val createCalls = mutableListOf<String>()

        fun setServers(list: List<McpServer>) { _servers.value = list }
        override suspend fun refreshServers() { if (shouldFail) throw Exception("Failed") }
        override suspend fun deleteServer(id: String) { deleteCalls.add(id); _servers.value = _servers.value.filter { it.id != id } }
        override suspend fun createServer(params: McpServerCreateParams): McpServer {
            createCalls.add(params.serverName)
            return TestData.mcpServer(serverName = params.serverName)
        }
    }

    private class FakeToolRepo : ToolRepository(FakeToolApi()) {
        private val _tools = MutableStateFlow<List<Tool>>(emptyList())
        fun setTools(list: List<Tool>) { _tools.value = list }
        override fun getTools(): Flow<List<Tool>> = _tools
        override suspend fun refreshTools() {}
    }
}
