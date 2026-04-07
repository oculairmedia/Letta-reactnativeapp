package com.letta.mobile.ui.screens.config

import app.cash.turbine.test
import com.letta.mobile.data.model.LettaConfig
import com.letta.mobile.data.repository.SettingsRepository
import com.letta.mobile.testutil.TestData
import com.letta.mobile.ui.common.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class ConfigListViewModelTest {

    private lateinit var fakeRepo: FakeSettingsRepo
    private lateinit var viewModel: ConfigListViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeSettingsRepo()
        viewModel = ConfigListViewModel(fakeRepo)
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `loadConfigs maps configs with active flag`() = runTest {
        val config1 = TestData.lettaConfig(id = "c1")
        val config2 = TestData.lettaConfig(id = "c2", mode = LettaConfig.Mode.SELF_HOSTED)
        fakeRepo.setConfigs(listOf(config1, config2), activeId = "c1")
        viewModel.loadConfigs()
        viewModel.uiState.test {
            val state = awaitItem() as UiState.Success
            assertEquals(2, state.data.configs.size)
            assertTrue(state.data.configs.first { it.id == "c1" }.isActive)
        }
    }

    @Test
    fun `loadConfigs sets Error on failure`() = runTest {
        fakeRepo.shouldFail = true
        viewModel.loadConfigs()
        viewModel.uiState.test { assertTrue(awaitItem() is UiState.Error) }
    }

    @Test
    fun `setActiveConfig calls repo`() = runTest {
        val config = TestData.lettaConfig(id = "c1")
        fakeRepo.setConfigs(listOf(config), activeId = null)
        viewModel.loadConfigs()
        viewModel.setActiveConfig("c1")
        assertEquals("c1", fakeRepo.lastSetActiveId)
    }

    @Test
    fun `deleteConfig calls repo`() = runTest {
        fakeRepo.setConfigs(listOf(TestData.lettaConfig(id = "c1")), activeId = "c1")
        viewModel.loadConfigs()
        viewModel.deleteConfig("c1")
        assertEquals("c1", fakeRepo.lastDeletedId)
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    private class FakeSettingsRepo : SettingsRepository(null as android.content.Context) {
        private val _configs = MutableStateFlow<List<LettaConfig>>(emptyList())
        private val _activeConfig = MutableStateFlow<LettaConfig?>(null)
        override val configs: StateFlow<List<LettaConfig>> = _configs.asStateFlow()
        override val activeConfig: StateFlow<LettaConfig?> = _activeConfig.asStateFlow()
        var shouldFail = false
        var lastSetActiveId: String? = null
        var lastDeletedId: String? = null

        fun setConfigs(list: List<LettaConfig>, activeId: String?) {
            _configs.value = list
            _activeConfig.value = list.find { it.id == activeId }
        }

        override suspend fun setActiveConfigId(id: String) {
            if (shouldFail) throw Exception("Failed")
            lastSetActiveId = id
            _activeConfig.value = _configs.value.find { it.id == id }
        }

        override suspend fun deleteConfig(id: String) {
            if (shouldFail) throw Exception("Failed")
            lastDeletedId = id
            _configs.value = _configs.value.filter { it.id != id }
        }
    }
}
