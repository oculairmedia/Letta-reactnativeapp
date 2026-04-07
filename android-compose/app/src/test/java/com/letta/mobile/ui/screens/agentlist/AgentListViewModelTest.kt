package com.letta.mobile.ui.screens.agentlist

import app.cash.turbine.test
import com.letta.mobile.data.model.Agent
import com.letta.mobile.data.model.AgentCreateParams
import com.letta.mobile.data.model.AgentUpdateParams
import com.letta.mobile.data.repository.AgentRepository
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
class AgentListViewModelTest {

    private lateinit var fakeRepository: FakeAgentRepository
    private lateinit var viewModel: AgentListViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAgentRepository()
        viewModel = AgentListViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadAgents_setsUiStateSuccess_withAgentsFromRepository() = runTest {
        val testAgents = listOf(
            Agent(id = "1", name = "Agent 1"),
            Agent(id = "2", name = "Agent 2")
        )
        fakeRepository.setAgents(testAgents)

        viewModel.loadAgents()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is UiState.Success)
            val successState = (state as UiState.Success).data
            assertEquals(2, successState.agents.size)
            assertEquals("Agent 1", successState.agents[0].name)
            assertEquals("Agent 2", successState.agents[1].name)
        }
    }

    @Test
    fun loadAgents_setsUiStateSuccess_withEmptyList() = runTest {
        fakeRepository.setAgents(emptyList())

        viewModel.loadAgents()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is UiState.Success)
            val successState = (state as UiState.Success).data
            assertEquals(0, successState.agents.size)
        }
    }

    @Test
    fun loadAgents_setsUiStateError_whenRepositoryThrows() = runTest {
        fakeRepository.shouldThrowOnRefresh = true

        viewModel.loadAgents()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is UiState.Error)
            val errorState = state as UiState.Error
            assertEquals("Repository error", errorState.message)
        }
    }

    @Test
    fun deleteAgent_callsRepository_andReloads() = runTest {
        val testAgents = listOf(
            Agent(id = "1", name = "Agent 1"),
            Agent(id = "2", name = "Agent 2"),
            Agent(id = "3", name = "Agent 3")
        )
        fakeRepository.setAgents(testAgents)
        viewModel.loadAgents()

        viewModel.deleteAgent("2")

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is UiState.Success)
            val successState = (state as UiState.Success).data
            assertEquals(2, successState.agents.size)
            assertEquals("Agent 1", successState.agents[0].name)
            assertEquals("Agent 3", successState.agents[1].name)
        }
    }

    @Test
    fun deleteAgent_setsUiStateError_whenRepositoryThrows() = runTest {
        fakeRepository.setAgents(listOf(Agent(id = "1", name = "Agent 1")))
        fakeRepository.shouldThrowOnDelete = true
        viewModel.loadAgents()

        viewModel.deleteAgent("1")

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is UiState.Error)
            val errorState = state as UiState.Error
            assertEquals("Delete failed", errorState.message)
        }
    }

    @Test
    fun createAgent_callsRepository_invokesOnSuccess_andReloads() = runTest {
        fakeRepository.setAgents(emptyList())
        viewModel.loadAgents()

        var capturedAgentId = ""
        viewModel.createAgent("New Agent") { agentId ->
            capturedAgentId = agentId
        }

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is UiState.Success)
            val successState = (state as UiState.Success).data
            assertEquals(1, successState.agents.size)
            assertEquals("New Agent", successState.agents[0].name)
            assertTrue(capturedAgentId.isNotEmpty())
        }
    }

    @Test
    fun createAgent_setsUiStateError_whenRepositoryThrows() = runTest {
        fakeRepository.shouldThrowOnCreate = true
        viewModel.loadAgents()

        var onSuccessCalled = false
        viewModel.createAgent("New Agent") {
            onSuccessCalled = true
        }

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is UiState.Error)
            val errorState = state as UiState.Error
            assertEquals("Create failed", errorState.message)
            assertEquals(false, onSuccessCalled)
        }
    }

    @Test
    fun refresh_updatesAgents_andSetsIsRefreshingFlag() = runTest {
        val initialAgents = listOf(Agent(id = "1", name = "Agent 1"))
        fakeRepository.setAgents(initialAgents)
        viewModel.loadAgents()

        val updatedAgents = listOf(
            Agent(id = "1", name = "Agent 1"),
            Agent(id = "2", name = "Agent 2")
        )
        fakeRepository.setAgents(updatedAgents)

        viewModel.refresh()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is UiState.Success)
            val successState = (state as UiState.Success).data
            assertEquals(2, successState.agents.size)
            assertEquals(false, successState.isRefreshing)
        }
    }

    @Test
    fun updateSearchQuery_updatesSearchQueryInState() = runTest {
        fakeRepository.setAgents(listOf(Agent(id = "1", name = "Agent 1")))
        viewModel.loadAgents()

        viewModel.updateSearchQuery("test query")

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is UiState.Success)
            val successState = (state as UiState.Success).data
            assertEquals("test query", successState.searchQuery)
        }
    }

    private class FakeAgentRepository : AgentRepository(null!!) {
        private val _agents = MutableStateFlow<List<Agent>>(emptyList())
        override val agents: StateFlow<List<Agent>> = _agents.asStateFlow()

        var shouldThrowOnRefresh = false
        var shouldThrowOnCreate = false
        var shouldThrowOnDelete = false

        fun setAgents(agentList: List<Agent>) {
            _agents.value = agentList
        }

        override suspend fun refreshAgents() {
            if (shouldThrowOnRefresh) {
                throw Exception("Repository error")
            }
        }

        override fun getAgent(id: String): Flow<Agent> {
            throw UnsupportedOperationException()
        }

        override fun getAgentPolling(id: String): Flow<Agent> {
            throw UnsupportedOperationException()
        }

        override suspend fun createAgent(params: AgentCreateParams): Agent {
            if (shouldThrowOnCreate) {
                throw Exception("Create failed")
            }
            val newAgent = Agent(
                id = "agent-${System.currentTimeMillis()}",
                name = params.name ?: "Unnamed"
            )
            val updatedList = _agents.value + newAgent
            _agents.value = updatedList
            return newAgent
        }

        override suspend fun updateAgent(id: String, params: AgentUpdateParams): Agent {
            throw UnsupportedOperationException()
        }

        override suspend fun deleteAgent(id: String) {
            if (shouldThrowOnDelete) {
                throw Exception("Delete failed")
            }
            val updatedList = _agents.value.filter { it.id != id }
            _agents.value = updatedList
        }
    }
}
