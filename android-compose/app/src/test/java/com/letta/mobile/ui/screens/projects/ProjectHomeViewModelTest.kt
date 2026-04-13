package com.letta.mobile.ui.screens.projects

import com.letta.mobile.data.api.ApiException
import com.letta.mobile.data.api.ProjectApi
import com.letta.mobile.data.model.ProjectCatalog
import com.letta.mobile.data.model.ProjectSummary
import com.letta.mobile.data.repository.ProjectRepository
import com.letta.mobile.ui.common.UiState
import io.mockk.mockk
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
class ProjectHomeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeApi: FakeProjectApi
    private lateinit var repository: ProjectRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeApi = FakeProjectApi()
        repository = ProjectRepository(fakeApi)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadProjects sorts projects by newest activity then name`() = runTest {
        fakeApi.projects += listOf(
            project(
                identifier = "beta",
                name = "Beta",
                updatedAt = "2026-04-10T10:00:00Z",
            ),
            project(
                identifier = "alpha-zulu",
                name = "Zulu",
                updatedAt = "2026-04-11T10:00:00Z",
            ),
            project(
                identifier = "alpha-echo",
                name = "Echo",
                updatedAt = "2026-04-11T10:00:00Z",
            ),
        )

        val viewModel = ProjectHomeViewModel(repository)

        val state = viewModel.uiState.value as UiState.Success
        assertEquals(listOf("Echo", "Zulu", "Beta"), state.data.projects.map { it.name })
        assertEquals(listOf("listProjects"), fakeApi.calls)
    }

    @Test
    fun `refresh forces a second project fetch`() = runTest {
        fakeApi.projects += project(identifier = "alpha", name = "Alpha")
        val viewModel = ProjectHomeViewModel(repository)

        viewModel.refresh()

        assertEquals(listOf("listProjects", "listProjects"), fakeApi.calls)
        val state = viewModel.uiState.value as UiState.Success
        assertEquals(false, state.data.isRefreshing)
    }

    @Test
    fun `selectProject tracks current project from loaded state`() = runTest {
        fakeApi.projects += listOf(
            project(identifier = "alpha", name = "Alpha"),
            project(identifier = "beta", name = "Beta"),
        )
        val viewModel = ProjectHomeViewModel(repository)

        viewModel.selectProject("beta")

        assertEquals("beta", (viewModel.uiState.value as UiState.Success).data.selectedProjectId)
        assertEquals("Beta", viewModel.currentProject()?.name)
    }

    @Test
    fun `loadProjects maps repository failures into ui error state`() = runTest {
        fakeApi.shouldFail = true

        val viewModel = ProjectHomeViewModel(repository)

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Error)
        assertEquals("Server error. Try again later.", (state as UiState.Error).message)
    }

    private fun project(
        identifier: String,
        name: String,
        updatedAt: String? = null,
    ) = ProjectSummary(
        identifier = identifier,
        name = name,
        updatedAt = updatedAt,
        lettaAgentId = "agent-$identifier",
    )

    private class FakeProjectApi : ProjectApi(mockk(relaxed = true)) {
        var projects = mutableListOf<ProjectSummary>()
        var shouldFail = false
        val calls = mutableListOf<String>()

        override suspend fun listProjects(): ProjectCatalog {
            calls.add("listProjects")
            if (shouldFail) throw ApiException(500, "Server error")
            return ProjectCatalog(total = projects.size, projects = projects.toList())
        }

        override suspend fun getProject(identifier: String): ProjectSummary {
            calls.add("getProject:$identifier")
            if (shouldFail) throw ApiException(500, "Server error")
            return projects.firstOrNull { it.identifier == identifier }
                ?: throw ApiException(404, "Not found")
        }
    }
}
