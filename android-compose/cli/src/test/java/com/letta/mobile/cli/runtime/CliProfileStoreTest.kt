package com.letta.mobile.cli.runtime

import java.nio.file.Files
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CliProfileStoreTest {
    @Test
    fun `upsert stores active profile and resolves connection`() {
        val path = Files.createTempDirectory("cli-profile-store").resolve("profiles.json")
        val store = CliProfileStore(path)

        store.upsert(
            CliProfile(
                name = "dev",
                baseUrl = "https://letta.example",
                token = "token-1",
                defaultAgentId = "agt-1",
                defaultConversationId = "conv-1",
            ),
            makeActive = true,
        )

        val document = store.load()
        assertEquals("dev", document.activeProfile)
        assertEquals("agt-1", document.profiles.single().defaultAgentId)

        val connection = store.resolve(profileName = null, explicitBaseUrl = null, explicitToken = null)
        assertEquals("dev", connection.profileName)
        assertEquals("https://letta.example", connection.baseUrl)
        assertEquals("token-1", connection.token)
        assertEquals("conv-1", connection.profile?.defaultConversationId)
    }

    @Test
    fun `explicit connection flags override profile values`() {
        val path = Files.createTempDirectory("cli-profile-store").resolve("profiles.json")
        val store = CliProfileStore(path)
        store.upsert(CliProfile(name = "dev", baseUrl = "https://profile", token = "profile-token"), makeActive = true)

        val connection = store.resolve(
            profileName = null,
            explicitBaseUrl = "https://flag",
            explicitToken = "flag-token",
        )

        assertEquals("https://flag", connection.baseUrl)
        assertEquals("flag-token", connection.token)
    }

    @Test
    fun `delete active profile falls back to first remaining profile`() {
        val path = Files.createTempDirectory("cli-profile-store").resolve("profiles.json")
        val store = CliProfileStore(path)
        store.upsert(CliProfile(name = "dev"), makeActive = true)
        store.upsert(CliProfile(name = "prod"), makeActive = false)

        val document = store.delete("dev")

        assertEquals("prod", document.activeProfile)
        assertEquals(listOf("prod"), document.profiles.map { it.name })
    }

    @Test
    fun `empty store resolves default base url without token`() {
        val path = Files.createTempDirectory("cli-profile-store").resolve("profiles.json")
        val connection = CliProfileStore(path).resolve(
            profileName = null,
            explicitBaseUrl = null,
            explicitToken = null,
        )

        assertEquals(CliProfileStore.DEFAULT_BASE_URL, connection.baseUrl)
        assertNull(connection.token)
    }
}
