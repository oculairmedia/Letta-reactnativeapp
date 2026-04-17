package com.letta.poc

import java.time.Instant
import kotlin.test.*

class TimelineTest {

    private fun local(otid: String, pos: Double, content: String = "msg"): TimelineEvent.Local =
        TimelineEvent.Local(
            position = pos,
            otid = otid,
            content = content,
            role = Role.USER,
            sentAt = Instant.now(),
            deliveryState = DeliveryState.SENDING,
        )

    private fun confirmed(otid: String, pos: Double, type: MessageType = MessageType.ASSISTANT): TimelineEvent.Confirmed =
        TimelineEvent.Confirmed(
            position = pos,
            otid = otid,
            content = "confirmed",
            serverId = "server-$otid",
            messageType = type,
            date = Instant.now(),
            runId = null,
            stepId = null,
        )

    @Test
    fun `empty timeline is valid`() {
        val t = Timeline("c1")
        assertEquals(0, t.events.size)
    }

    @Test
    fun `append respects ordering`() {
        val t = Timeline("c1")
            .append(local("a", 1.0))
            .append(confirmed("b", 2.0))
        assertEquals(2, t.events.size)
        assertEquals("a", t.events[0].otid)
        assertEquals("b", t.events[1].otid)
    }

    @Test
    fun `append rejects out-of-order`() {
        val t = Timeline("c1").append(local("a", 5.0))
        assertFailsWith<IllegalArgumentException> {
            t.append(local("b", 3.0))  // earlier position
        }
    }

    @Test
    fun `append rejects duplicate otid`() {
        val t = Timeline("c1").append(local("dup", 1.0))
        assertFailsWith<IllegalArgumentException> {
            t.append(confirmed("dup", 2.0))
        }
    }

    @Test
    fun `replaceLocal swaps in place preserving position`() {
        val t = Timeline("c1")
            .append(local("user-1", 1.0, "hi"))
            .append(confirmed("reply-1", 2.0))

        val confirmedUser = confirmed("user-1", 99.0, MessageType.USER)  // intentional wrong pos
        val updated = t.replaceLocal("user-1", confirmedUser)

        // Position of "user-1" preserved at 1.0
        val first = updated.events[0]
        assertTrue(first is TimelineEvent.Confirmed)
        assertEquals(1.0, first.position)
        assertEquals("user-1", first.otid)
        // Other event unchanged
        assertEquals(2.0, updated.events[1].position)
    }

    @Test
    fun `replaceLocal with unknown otid appends (backfill case)`() {
        val t = Timeline("c1").append(local("a", 1.0))
        val updated = t.replaceLocal("stranger", confirmed("stranger", 5.0))
        assertEquals(2, updated.events.size)
    }

    @Test
    fun `insertOrdered places event at correct position`() {
        val t = Timeline("c1")
            .append(confirmed("a", 1.0))
            .append(confirmed("c", 3.0))
        val updated = t.insertOrdered(confirmed("b", 2.0))
        assertEquals(listOf("a", "b", "c"), updated.events.map { it.otid })
    }

    @Test
    fun `insertOrdered dedupes by otid`() {
        val t = Timeline("c1").append(confirmed("x", 1.0))
        val updated = t.insertOrdered(confirmed("x", 99.0))
        assertEquals(1, updated.events.size)
        assertEquals(1.0, updated.events[0].position)  // original kept
    }

    @Test
    fun `markSent transitions local state`() {
        val t = Timeline("c1").append(local("a", 1.0))
        val updated = t.markSent("a")
        val event = updated.events[0] as TimelineEvent.Local
        assertEquals(DeliveryState.SENT, event.deliveryState)
    }

    @Test
    fun `markFailed transitions local state`() {
        val t = Timeline("c1").append(local("a", 1.0))
        val updated = t.markFailed("a")
        val event = updated.events[0] as TimelineEvent.Local
        assertEquals(DeliveryState.FAILED, event.deliveryState)
    }

    @Test
    fun `markSent no-op on confirmed event`() {
        val t = Timeline("c1").append(confirmed("a", 1.0))
        val updated = t.markSent("a")
        assertEquals(t, updated)  // unchanged
    }

    @Test
    fun `nextLocalPosition increments`() {
        val t0 = Timeline("c1")
        assertEquals(1.0, t0.nextLocalPosition())

        val t1 = t0.append(local("a", 1.0))
        assertEquals(2.0, t1.nextLocalPosition())

        val t2 = t1.append(confirmed("b", 5.0))
        assertEquals(6.0, t2.nextLocalPosition())
    }

    // === Scenario-level tests (simulate the 8 POC validation scenarios on Timeline only) ===

    @Test
    fun `scenario - basic send, confirm, reply`() {
        // 1. User sends "hi" → Local appended
        val userOtid = "user-hi"
        var t = Timeline("c1").append(local(userOtid, 1.0, "hi"))

        // 2. Assistant streamed response arrives
        val assistantOtid = "assistant-reply-01"
        t = t.append(confirmed(assistantOtid, 2.0, MessageType.ASSISTANT))

        // 3. Reconcile: user message confirmed by server
        val userConfirmed = confirmed(userOtid, 999.0, MessageType.USER)  // position doesn't matter
        t = t.replaceLocal(userOtid, userConfirmed)

        // 4. Verify ordering preserved
        assertEquals(userOtid, t.events[0].otid)
        assertEquals(assistantOtid, t.events[1].otid)
        assertTrue(t.events[0] is TimelineEvent.Confirmed)
    }

    @Test
    fun `scenario - rapid consecutive sends preserve order`() {
        var t = Timeline("c1")
        val otids = (1..5).map { "msg-$it" }
        otids.forEach {
            t = t.append(local(it, t.nextLocalPosition()))
        }
        // All 5 present, in order
        assertEquals(otids, t.events.map { it.otid })
    }

    @Test
    fun `scenario - identical content sent twice produces two events`() {
        val t = Timeline("c1")
            .append(local("otid-1", 1.0, "hi"))
            .append(local("otid-2", 2.0, "hi"))
        assertEquals(2, t.events.size)
        assertEquals("hi", t.events[0].content)
        assertEquals("hi", t.events[1].content)
    }

    @Test
    fun `scenario - pagination backfill inserts in correct position`() {
        var t = Timeline("c1")
            .append(confirmed("recent-1", 100.0))
            .append(confirmed("recent-2", 101.0))
        // Backfill: older message at position 50
        t = t.insertOrdered(confirmed("old-1", 50.0))
        assertEquals(listOf("old-1", "recent-1", "recent-2"), t.events.map { it.otid })
    }
}
