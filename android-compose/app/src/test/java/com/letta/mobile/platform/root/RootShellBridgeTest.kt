package com.letta.mobile.platform.root

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
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

    @Test
    fun `root command classifier separates read write destructive and persistence commands`() {
        assertEquals(RootCommandRiskCategory.ReadOnly, RootShellCommandClassifier.classify("id -u"))
        assertEquals(RootCommandRiskCategory.Write, RootShellCommandClassifier.classify("settings put global airplane_mode_on 0"))
        assertEquals(RootCommandRiskCategory.Destructive, RootShellCommandClassifier.classify("rm -rf /data/local/tmp/demo"))
        assertEquals(RootCommandRiskCategory.PersistenceChanging, RootShellCommandClassifier.classify("pm install /sdcard/demo.apk"))
    }

    @Test
    fun `audit logger records command metadata and truncates output`() = runTest {
        val logger = InMemoryRootShellAuditLogger()
        val stdout = "x".repeat(RootShellDefaults.AUDIT_OUTPUT_LIMIT_CHARS + 20)

        logger.record(
            request = RootShellCommandRequest(
                command = "settings put global test_flag 1",
                cwd = "/data/local/tmp",
                approvalId = "approval-1",
                agentId = "agent-1",
                sessionId = "session-1",
            ),
            result = RootShellCommandResult(
                exitCode = 0,
                stdout = stdout,
                stderr = "",
                stdoutTruncated = false,
                stderrTruncated = false,
                timedOut = false,
                durationMs = 12,
                providerHint = "KernelSU",
            ),
        )

        val event = logger.recordedEvents().single()
        assertEquals("settings put global test_flag 1", event.command)
        assertEquals("/data/local/tmp", event.cwd)
        assertEquals("approval-1", event.approvalId)
        assertEquals("agent-1", event.agentId)
        assertEquals("session-1", event.sessionId)
        assertEquals(0, event.exitCode)
        assertEquals(RootCommandRiskCategory.Write, event.riskCategory)
        assertTrue(event.stdoutTruncated)
        assertEquals(RootShellDefaults.AUDIT_OUTPUT_LIMIT_CHARS + 1, event.stdout.length)
    }

}
