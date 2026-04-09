package com.letta.mobile.ui.screens.runs

import com.letta.mobile.data.model.Run
import com.letta.mobile.data.model.RunRequestConfig
import com.letta.mobile.data.repository.RunRepository
import com.letta.mobile.testutil.FakeRunApi
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
class RunMonitorViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeApi: FakeRunApi
    private lateinit var repository: RunRepository
    private lateinit var viewModel: RunMonitorViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeApi = FakeRunApi()
        fakeApi.runs.addAll(
            listOf(
                sampleRun("r1", "running"),
                sampleRun("r2", "cancelled"),
            )
        )
        repository = RunRepository(fakeApi)
        viewModel = RunMonitorViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadRuns populates state`() = runTest {
        viewModel.loadRuns()

        val state = viewModel.uiState.value as com.letta.mobile.ui.common.UiState.Success
        assertEquals(2, state.data.runs.size)
    }

    @Test
    fun `toggleActiveOnly filters runs through repository`() = runTest {
        viewModel.toggleActiveOnly(true)

        val state = viewModel.uiState.value as com.letta.mobile.ui.common.UiState.Success
        assertTrue(state.data.activeOnly)
        assertEquals(1, state.data.runs.size)
        assertEquals("running", state.data.runs.first().status)
    }

    @Test
    fun `updateSearchQuery filters runs locally`() = runTest {
        viewModel.loadRuns()
        viewModel.updateSearchQuery("cancelled")

        val filtered = viewModel.getFilteredRuns()
        assertEquals(1, filtered.size)
        assertEquals("r2", filtered.first().id)
    }

    @Test
    fun `inspectRun loads detailed run`() = runTest {
        viewModel.inspectRun("r1")

        val state = viewModel.uiState.value as com.letta.mobile.ui.common.UiState.Success
        assertEquals("r1", state.data.selectedRun?.id)
    }
}

private fun sampleRun(id: String, status: String) = Run(
    id = id,
    agentId = "agent-1",
    conversationId = "conv-1",
    status = status,
    background = status == "running",
    stopReason = if (status == "cancelled") "cancelled" else null,
    createdAt = "2026-04-09T10:00:00Z",
    requestConfig = RunRequestConfig(useAssistantMessage = true),
)
