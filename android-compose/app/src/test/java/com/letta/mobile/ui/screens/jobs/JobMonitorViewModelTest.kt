package com.letta.mobile.ui.screens.jobs

import com.letta.mobile.data.model.Job
import com.letta.mobile.data.repository.JobRepository
import com.letta.mobile.testutil.FakeJobApi
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
class JobMonitorViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeApi: FakeJobApi
    private lateinit var repository: JobRepository
    private lateinit var viewModel: JobMonitorViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeApi = FakeJobApi()
        fakeApi.jobs.addAll(
            listOf(
                sampleJob("job-1", "running"),
                sampleJob("job-2", "completed"),
            )
        )
        repository = JobRepository(fakeApi)
        viewModel = JobMonitorViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadJobs populates state`() = runTest {
        viewModel.loadJobs()

        val state = viewModel.uiState.value as com.letta.mobile.ui.common.UiState.Success
        assertEquals(2, state.data.jobs.size)
    }

    @Test
    fun `toggleActiveOnly filters jobs through repository`() = runTest {
        viewModel.toggleActiveOnly(true)

        val state = viewModel.uiState.value as com.letta.mobile.ui.common.UiState.Success
        assertTrue(state.data.activeOnly)
        assertEquals(1, state.data.jobs.size)
        assertEquals("running", state.data.jobs.first().status)
    }

    @Test
    fun `updateSearchQuery filters jobs locally`() = runTest {
        viewModel.loadJobs()
        viewModel.updateSearchQuery("completed")

        val filtered = viewModel.getFilteredJobs()
        assertEquals(1, filtered.size)
        assertEquals("job-2", filtered.first().id)
    }

    @Test
    fun `inspectJob loads detailed job`() = runTest {
        viewModel.inspectJob("job-1")

        val state = viewModel.uiState.value as com.letta.mobile.ui.common.UiState.Success
        assertEquals("job-1", state.data.selectedJob?.id)
    }

    @Test
    fun `cancelJob updates selected job state`() = runTest {
        viewModel.inspectJob("job-1")

        viewModel.cancelJob("job-1")

        val state = viewModel.uiState.value as com.letta.mobile.ui.common.UiState.Success
        assertEquals("cancelled", state.data.selectedJob?.status)
    }

    @Test
    fun `loadJobs includes terminal jobs when active filter is off`() = runTest {
        viewModel.loadJobs()

        val state = viewModel.uiState.value as com.letta.mobile.ui.common.UiState.Success
        assertEquals(listOf("job-1", "job-2"), state.data.jobs.map { it.id })
    }

    @Test
    fun `cancelJob removes cancelled job from active-only list`() = runTest {
        viewModel.toggleActiveOnly(true)
        viewModel.inspectJob("job-1")

        viewModel.cancelJob("job-1")

        val state = viewModel.uiState.value as com.letta.mobile.ui.common.UiState.Success
        assertTrue(state.data.jobs.isEmpty())
        assertEquals(null, state.data.selectedJob)
    }

    @Test
    fun `deleteJob removes job from list`() = runTest {
        viewModel.deleteJob("job-1")

        val state = viewModel.uiState.value as com.letta.mobile.ui.common.UiState.Success
        assertEquals(1, state.data.jobs.size)
        assertEquals("job-2", state.data.jobs.first().id)
    }
}

private fun sampleJob(id: String, status: String) = Job(
    id = id,
    status = status,
    agentId = "agent-1",
    jobType = "job",
    userId = "user-1",
    createdAt = "2026-04-09T10:00:00Z",
)
