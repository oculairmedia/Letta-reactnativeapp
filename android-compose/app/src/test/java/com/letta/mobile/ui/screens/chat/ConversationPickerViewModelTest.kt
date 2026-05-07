package com.letta.mobile.ui.screens.chat

import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.jupiter.api.Tag

@OptIn(ExperimentalCoroutinesApi::class)
@Tag("unit")
class ConversationPickerViewModelTest {

    private val repository: com.letta.mobile.data.repository.ConversationRepository = mockk(relaxed = true)

    @Test
    fun toggleSelection_addsId() = runTest {
        val vm = ConversationPickerViewModel(repository)

        vm.toggleSelection("conv-1")
        val result = vm.selectedIds.first()

        assertTrue(result.contains("conv-1"))
    }

    @Test
    fun toggleSelection_removesId() = runTest {
        val vm = ConversationPickerViewModel(repository)

        vm.toggleSelection("conv-1")
        vm.toggleSelection("conv-1")
        val result = vm.selectedIds.first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun toggleSelection_supportsMultipleIds() = runTest {
        val vm = ConversationPickerViewModel(repository)

        vm.toggleSelection("conv-1")
        vm.toggleSelection("conv-2")
        vm.toggleSelection("conv-3")

        assertEquals(setOf("conv-1", "conv-2", "conv-3"), vm.selectedIds.first())
    }

    @Test
    fun clearSelection_emptiesSelected() = runTest {
        val vm = ConversationPickerViewModel(repository)

        vm.toggleSelection("conv-1")
        vm.toggleSelection("conv-2")
        vm.clearSelection()

        assertTrue(vm.selectedIds.first().isEmpty())
    }
}
