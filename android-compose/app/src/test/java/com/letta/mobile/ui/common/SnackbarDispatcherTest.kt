package com.letta.mobile.ui.common

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SnackbarDispatcherTest {

    @Test
    fun `dispatch string message`() = runTest {
        val dispatcher = SnackbarDispatcher()
        dispatcher.dispatch("Hello")
        val msg = dispatcher.messages.first()
        assertEquals("Hello", msg.message)
        assertNull(msg.actionLabel)
        assertNull(msg.onAction)
    }

    @Test
    fun `dispatch full message with action`() = runTest {
        val dispatcher = SnackbarDispatcher()
        var actionCalled = false
        dispatcher.dispatch(SnackbarMessage("Deleted", "Undo") { actionCalled = true })
        val msg = dispatcher.messages.first()
        assertEquals("Deleted", msg.message)
        assertEquals("Undo", msg.actionLabel)
        msg.onAction?.invoke()
        assertEquals(true, actionCalled)
    }

    @Test
    fun `multiple dispatches queued`() = runTest {
        val dispatcher = SnackbarDispatcher()
        dispatcher.dispatch("First")
        dispatcher.dispatch("Second")
        assertEquals("First", dispatcher.messages.first().message)
        assertEquals("Second", dispatcher.messages.first().message)
    }
}
