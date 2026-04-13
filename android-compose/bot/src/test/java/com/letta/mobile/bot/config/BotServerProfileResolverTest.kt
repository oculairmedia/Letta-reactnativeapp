package com.letta.mobile.bot.config

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking

class BotServerProfileResolverTest : WordSpec({
    "BotServerProfileResolver" should {
        "prefer explicitly selected profile over active profile" {
            val profileStore = FakeBotServerProfileStore(
                storedProfiles = listOf(
                    BotServerProfile(id = "active", displayName = "Active", baseUrl = "https://active.example", isActive = true),
                    BotServerProfile(id = "selected", displayName = "Selected", baseUrl = "https://selected.example", isActive = false),
                )
            )
            val resolver = BotServerProfileResolver(profileStore)

            val resolved = runBlocking {
                resolver.resolve(
                    BotConfig(
                        id = "bot-1",
                        agentId = "agent-1",
                        mode = BotConfig.Mode.REMOTE,
                        serverProfileId = "selected",
                    )
                )
            }

            resolved!!.profileId shouldBe "selected"
            resolved.baseUrl shouldBe "https://selected.example"
        }

        "fall back to active profile when explicit profile is absent" {
            val profileStore = FakeBotServerProfileStore(
                storedProfiles = listOf(
                    BotServerProfile(id = "active", displayName = "Active", baseUrl = "https://active.example", authToken = "token", isActive = true),
                )
            )
            val resolver = BotServerProfileResolver(profileStore)

            val resolved = runBlocking {
                resolver.resolve(
                    BotConfig(
                        id = "bot-1",
                        agentId = "agent-1",
                        mode = BotConfig.Mode.REMOTE,
                    )
                )
            }

            resolved!!.profileId shouldBe "active"
            resolved.authToken shouldBe "token"
        }

        "fall back to inline remote config when no saved profile exists" {
            val resolver = BotServerProfileResolver(FakeBotServerProfileStore(emptyList()))

            val resolved = runBlocking {
                resolver.resolve(
                    BotConfig(
                        id = "bot-1",
                        agentId = "agent-1",
                        mode = BotConfig.Mode.REMOTE,
                        remoteUrl = "https://inline.example",
                        remoteToken = "inline-token",
                    )
                )
            }

            resolved!!.profileId shouldBe null
            resolved.baseUrl shouldBe "https://inline.example"
            resolved.authToken shouldBe "inline-token"
        }
    }
})

private class FakeBotServerProfileStore(
    private val storedProfiles: List<BotServerProfile>,
) : IBotServerProfileStore {
    override val profiles = kotlinx.coroutines.flow.flowOf(storedProfiles)

    override suspend fun saveProfile(profile: BotServerProfile) = Unit

    override suspend fun deleteProfile(profileId: String) = Unit

    override suspend fun getAll(): List<BotServerProfile> = storedProfiles

    override suspend fun activateProfile(profileId: String) = Unit

    override suspend fun findById(profileId: String): BotServerProfile? = storedProfiles.firstOrNull { it.id == profileId }
    override suspend fun getActiveProfile(): BotServerProfile? = storedProfiles.firstOrNull { it.isActive }
}
