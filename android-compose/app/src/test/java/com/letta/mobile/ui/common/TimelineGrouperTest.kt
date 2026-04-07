package com.letta.mobile.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class TimelineGrouperTest {

    data class FakeMessage(val id: String, val role: String, val ts: String = "")

    private fun group(messages: List<FakeMessage>): List<Pair<FakeMessage, GroupPosition>> {
        return groupMessages(messages, getRole = { it.role }, getTimestamp = { it.ts })
    }

    @Test
    fun `empty list returns empty`() {
        assertEquals(emptyList<Pair<FakeMessage, GroupPosition>>(), group(emptyList()))
    }

    @Test
    fun `single message returns None`() {
        val msgs = listOf(FakeMessage("1", "user"))
        val result = group(msgs)
        assertEquals(1, result.size)
        assertEquals(GroupPosition.None, result[0].second)
    }

    @Test
    fun `two messages same role returns First Last`() {
        val msgs = listOf(FakeMessage("1", "user"), FakeMessage("2", "user"))
        val result = group(msgs)
        assertEquals(GroupPosition.First, result[0].second)
        assertEquals(GroupPosition.Last, result[1].second)
    }

    @Test
    fun `two messages different role returns None None`() {
        val msgs = listOf(FakeMessage("1", "user"), FakeMessage("2", "assistant"))
        val result = group(msgs)
        assertEquals(GroupPosition.None, result[0].second)
        assertEquals(GroupPosition.None, result[1].second)
    }

    @Test
    fun `three consecutive same role returns First Middle Last`() {
        val msgs = listOf(
            FakeMessage("1", "assistant"),
            FakeMessage("2", "assistant"),
            FakeMessage("3", "assistant"),
        )
        val result = group(msgs)
        assertEquals(GroupPosition.First, result[0].second)
        assertEquals(GroupPosition.Middle, result[1].second)
        assertEquals(GroupPosition.Last, result[2].second)
    }

    @Test
    fun `alternating roles all None`() {
        val msgs = listOf(
            FakeMessage("1", "user"),
            FakeMessage("2", "assistant"),
            FakeMessage("3", "user"),
            FakeMessage("4", "assistant"),
        )
        val result = group(msgs)
        result.forEach { assertEquals(GroupPosition.None, it.second) }
    }

    @Test
    fun `mixed groups assign correct positions`() {
        val msgs = listOf(
            FakeMessage("1", "user"),
            FakeMessage("2", "user"),
            FakeMessage("3", "assistant"),
            FakeMessage("4", "assistant"),
            FakeMessage("5", "assistant"),
            FakeMessage("6", "user"),
        )
        val result = group(msgs)
        assertEquals(GroupPosition.First, result[0].second)
        assertEquals(GroupPosition.Last, result[1].second)
        assertEquals(GroupPosition.First, result[2].second)
        assertEquals(GroupPosition.Middle, result[3].second)
        assertEquals(GroupPosition.Last, result[4].second)
        assertEquals(GroupPosition.None, result[5].second)
    }

    @Test
    fun `tool messages break groups`() {
        val msgs = listOf(
            FakeMessage("1", "assistant"),
            FakeMessage("2", "tool"),
            FakeMessage("3", "assistant"),
        )
        val result = group(msgs)
        assertEquals(GroupPosition.None, result[0].second)
        assertEquals(GroupPosition.None, result[1].second)
        assertEquals(GroupPosition.None, result[2].second)
    }

    @Test
    fun `preserves original message objects`() {
        val msgs = listOf(FakeMessage("a", "user"), FakeMessage("b", "user"))
        val result = group(msgs)
        assertEquals("a", result[0].first.id)
        assertEquals("b", result[1].first.id)
    }
}
