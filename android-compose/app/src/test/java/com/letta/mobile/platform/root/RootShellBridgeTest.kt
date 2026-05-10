package com.letta.mobile.platform.root

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class RootShellBridgeTest {
    @Test
    fun `noop bridge fails closed`() = runTest {
        val bridge = NoopRootShellBridge()

        val availability = bridge.peekAvailability()
        val result = bridge.execute(RootShellCommandRequest(command = "id"))

        assertEquals(RootShellAvailabilityStatus.UnsupportedBuild, availability.status)
        assertFalse(result.succeeded)
        assertEquals(null, result.exitCode)
    }

    @Test
    fun `command request rejects blank commands`() {
        assertThrows(IllegalArgumentException::class.java) {
            RootShellCommandRequest(command = "   ")
        }
    }

    @Test
    fun `command request rejects unsafe environment keys`() {
        assertThrows(IllegalArgumentException::class.java) {
            RootShellCommandRequest(command = "id", environment = mapOf("BAD-KEY" to "value"))
        }
    }
}
