package com.letta.mobile.domain

import com.letta.mobile.testutil.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AgentSearchTest {

    private lateinit var search: AgentSearch
    private val agents = listOf(
        TestData.agent(id = "1", name = "General Assistant", model = "letta/letta-free", tags = listOf("default", "chat"), description = "A general purpose agent"),
        TestData.agent(id = "2", name = "Code Helper", model = "openai/gpt-4o", tags = listOf("code", "programming"), description = "Helps with code"),
        TestData.agent(id = "3", name = "Research Bot", model = "anthropic/claude-3.5-sonnet", tags = listOf("research"), description = "Does research"),
        TestData.agent(id = "4", name = "Writer", model = "openai/gpt-4o", tags = listOf("writing"), description = "Creative writing assistant"),
    )

    @Before
    fun setup() {
        search = AgentSearch()
    }

    @Test
    fun `empty query returns all agents`() {
        val result = search.search(agents, "")
        assertEquals(agents.size, result.size)
    }

    @Test
    fun `blank query returns all agents`() {
        val result = search.search(agents, "   ")
        assertEquals(agents.size, result.size)
    }

    @Test
    fun `exact name match returns agent`() {
        val result = search.search(agents, "Code Helper")
        assertTrue(result.isNotEmpty())
        assertEquals("Code Helper", result.first().name)
    }

    @Test
    fun `partial name match returns agent`() {
        val result = search.search(agents, "General")
        assertTrue(result.isNotEmpty())
        assertEquals("General Assistant", result.first().name)
    }

    @Test
    fun `case insensitive search`() {
        val result = search.search(agents, "code helper")
        assertTrue(result.isNotEmpty())
        assertEquals("Code Helper", result.first().name)
    }

    @Test
    fun `tag matching returns agent`() {
        val result = search.search(agents, "programming")
        assertTrue(result.isNotEmpty())
        assertTrue(result.any { it.name == "Code Helper" })
    }

    @Test
    fun `model matching returns agents`() {
        val result = search.search(agents, "gpt-4o")
        assertTrue(result.size >= 2)
    }

    @Test
    fun `no match returns empty`() {
        val result = search.search(agents, "zzzznonexistent")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `empty agent list returns empty`() {
        val result = search.search(emptyList(), "test")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `results sorted by relevance`() {
        val result = search.search(agents, "Code")
        assertTrue(result.isNotEmpty())
        assertEquals("Code Helper", result.first().name)
    }
}
