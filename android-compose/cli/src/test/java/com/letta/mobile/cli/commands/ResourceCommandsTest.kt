package com.letta.mobile.cli.commands

import com.github.ajalt.clikt.core.UsageError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ResourceCommandsTest {
    @Test
    fun `path templates encode each placeholder as a path segment`() {
        val path = buildResourcePathTemplate(
            "/v1/agents/{agent_id}/tools/attach/{tool_id}",
            mapOf("agent_id" to "agent/one", "tool_id" to "tool one"),
        )

        assertEquals("/v1/agents/agent%2Fone/tools/attach/tool%20one", path)
    }

    @Test
    fun `body templates render placeholder values as json strings`() {
        val body = buildResourceBodyTemplate(
            """{"projectId":{project_id}}""",
            mapOf("project_id" to """proj"one"""),
        )

        assertEquals("""{"projectId":"proj\"one"}""", body)
    }

    @Test
    fun `missing template values fail before an http request is made`() {
        assertThrows(UsageError::class.java) {
            buildResourcePathTemplate("/v1/agents/{agent_id}", emptyMap())
        }
    }

    @Test
    fun `resource registry includes app admin surfaces`() {
        val names = resourceCommandNames()

        assertTrue(
            names.containsAll(
                setOf(
                    "agents",
                    "conversations",
                    "tools",
                    "blocks",
                    "archives",
                    "folders",
                    "groups",
                    "identities",
                    "schedules",
                    "mcp",
                    "runs",
                    "jobs",
                    "steps",
                    "models",
                    "providers",
                    "projects",
                    "project-work",
                )
            )
        )
    }
}
