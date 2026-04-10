package com.letta.mobile.ui.screens.jobs

import com.letta.mobile.data.model.Job
import com.letta.mobile.data.repository.JobRepository
import com.letta.mobile.testutil.FakeJobApi
import com.letta.mobile.ui.common.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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

        val state = awaitSuccessState()
        assertEquals(2, state.jobs.size)
    }

    @Test
    fun `toggleActiveOnly filters jobs through repository`() = runTest {
        viewModel.toggleActiveOnly(true)

        val state = awaitSuccessState()
        assertTrue(state.activeOnly)
        assertEquals(1, state.jobs.size)
        assertEquals("running", state.jobs.first().status)
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
        awaitSuccessState()
        viewModel.inspectJob("job-1")

        val state = awaitSuccessState()
        assertEquals("job-1", state.selectedJob?.id)
    }

    @Test
    fun `cancelJob updates selected job state`() = runTest {
        awaitSuccessState()
        viewModel.inspectJob("job-1")

        viewModel.cancelJob("job-1")

        val state = awaitSuccessState()
        assertEquals("cancelled", state.selectedJob?.status)
    }

    @Test
    fun `loadJobs includes terminal jobs when active filter is off`() = runTest {
        viewModel.loadJobs()

        val state = awaitSuccessState()
        assertEquals(listOf("job-1", "job-2"), state.jobs.map { it.id })
    }

    @Test
    fun `cancelJob removes cancelled job from active-only list`() = runTest {
        viewModel.toggleActiveOnly(true)
        awaitSuccessState()
        viewModel.inspectJob("job-1")
        awaitSuccessState()

        viewModel.cancelJob("job-1")

        val state = awaitSuccessState()
        assertTrue(state.jobs.isEmpty())
        assertEquals(null, state.selectedJob)
    }

    @Test
    fun `deleteJob removes job from list`() = runTest {
        awaitSuccessState()
        viewModel.deleteJob("job-1")

        val state = awaitSuccessState()
        assertEquals(1, state.jobs.size)
        assertEquals("job-2", state.jobs.first().id)
    }

    private suspend fun awaitSuccessState(): JobMonitorUiState {
        return viewModel.uiState.first { it is UiState.Success }.let { (it as UiState.Success).data }
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
