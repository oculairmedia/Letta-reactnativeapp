package com.letta.mobile.ui.screens.identities

import com.letta.mobile.data.model.Identity
import com.letta.mobile.data.model.IdentityCreateParams
import com.letta.mobile.data.repository.IdentityRepository
import com.letta.mobile.testutil.FakeIdentityApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IdentityListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeApi: FakeIdentityApi
    private lateinit var repository: IdentityRepository
    private lateinit var viewModel: IdentityListViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeApi = FakeIdentityApi()
        fakeApi.identities.addAll(
            listOf(
                sampleIdentity("identity-1", "user-1", "User One", "user"),
                sampleIdentity("identity-2", "org-1", "Org One", "org"),
            )
        )
        repository = IdentityRepository(fakeApi)
        viewModel = IdentityListViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadIdentities populates state`() = runTest {
        viewModel.loadIdentities()

        val state = viewModel.uiState.value as com.letta.mobile.ui.common.UiState.Success
        assertEquals(2, state.data.identities.size)
    }

    @Test
    fun `updateSearchQuery filters identities locally`() = runTest {
        viewModel.loadIdentities()
        viewModel.updateSearchQuery("org")

        val filtered = viewModel.getFilteredIdentities()
        assertEquals(1, filtered.size)
        assertEquals("identity-2", filtered.first().id)
    }

    @Test
    fun `inspectIdentity loads details`() = runTest {
        viewModel.inspectIdentity("identity-1")

        val state = viewModel.uiState.value as com.letta.mobile.ui.common.UiState.Success
        assertEquals("identity-1", state.data.selectedIdentity?.id)
    }

    @Test
    fun `createIdentity delegates to repository`() = runTest {
        viewModel.createIdentity(
            IdentityCreateParams(identifierKey = "user-2", name = "User Two", identityType = "user")
        )

        val state = viewModel.uiState.value as com.letta.mobile.ui.common.UiState.Success
        assertEquals(3, state.data.identities.size)
    }

    @Test
    fun `deleteIdentity removes identity from state`() = runTest {
        viewModel.deleteIdentity("identity-1")

        val state = viewModel.uiState.value as com.letta.mobile.ui.common.UiState.Success
        assertEquals(1, state.data.identities.size)
        assertEquals("identity-2", state.data.identities.first().id)
    }
}

private fun sampleIdentity(id: String, identifierKey: String, name: String, type: String) = Identity(
    id = id,
    identifierKey = identifierKey,
    name = name,
    identityType = type,
)
