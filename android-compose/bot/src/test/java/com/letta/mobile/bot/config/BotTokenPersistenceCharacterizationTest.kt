package com.letta.mobile.bot.config

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Tag

@Tag("unit")
class BotTokenPersistenceCharacterizationTest : WordSpec({

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    "BotConfig token migration" should {
        "move plaintext remote and API server tokens into secure storage and redact persisted JSON" {
            val tokenStore = FakeBotSecureTokenStore()
            val result = hydrateAndStoreBotConfigTokens(
                listOf(
                    BotConfig(
                        id = "bot-1",
                        displayName = "Remote bot",
                        mode = BotConfig.Mode.REMOTE,
                        remoteUrl = "https://bot.example.test",
                        remoteToken = "remote-secret-token",
                        apiServerEnabled = true,
                        apiServerToken = "api-server-secret-token",
                    ),
                ),
                tokenStore,
            )
            val hydrated = result.values.single()
            val raw = json.encodeToString(result.values.map(::sanitizeBotConfigTokens))
            val obj = json.parseToJsonElement(raw).jsonArray.single().jsonObject

            result.foundPlaintextTokens shouldBe true
            hydrated.remoteToken shouldBe "remote-secret-token"
            hydrated.apiServerToken shouldBe "api-server-secret-token"
            tokenStore[BotTokenKeys.configRemoteToken("bot-1")] shouldBe "remote-secret-token"
            tokenStore[BotTokenKeys.configApiServerToken("bot-1")] shouldBe "api-server-secret-token"
            obj.keys shouldContain "remote_token"
            obj.keys shouldContain "api_server_token"
            obj["remote_token"]?.jsonPrimitive?.contentOrNull shouldBe null
            obj["api_server_token"]?.jsonPrimitive?.contentOrNull shouldBe null
            raw.contains("remote-secret-token") shouldBe false
            raw.contains("api-server-secret-token") shouldBe false
        }

        "hydrate redacted configs from secure storage" {
            val tokenStore = FakeBotSecureTokenStore(
                BotTokenKeys.configRemoteToken("bot-1") to "remote-secret-token",
                BotTokenKeys.configApiServerToken("bot-1") to "api-server-secret-token",
            )

            val result = hydrateAndStoreBotConfigTokens(listOf(BotConfig(id = "bot-1")), tokenStore)
            val hydrated = result.values.single()

            result.foundPlaintextTokens shouldBe false
            hydrated.remoteToken shouldBe "remote-secret-token"
            hydrated.apiServerToken shouldBe "api-server-secret-token"
        }
    }

    "BotServerProfile token migration" should {
        "move plaintext auth tokens into secure storage and redact persisted JSON" {
            val tokenStore = FakeBotSecureTokenStore()
            val result = hydrateAndStoreBotServerProfileTokens(
                listOf(
                    BotServerProfile(
                        id = "profile-1",
                        displayName = "Self hosted",
                        baseUrl = "https://server.example.test",
                        authToken = "profile-secret-token",
                    ),
                ),
                tokenStore,
            )
            val hydrated = result.values.single()
            val raw = json.encodeToString(result.values.map(::sanitizeBotServerProfileToken))
            val obj = json.parseToJsonElement(raw).jsonArray.single().jsonObject

            result.foundPlaintextTokens shouldBe true
            hydrated.authToken shouldBe "profile-secret-token"
            tokenStore[BotTokenKeys.serverProfileAuthToken("profile-1")] shouldBe "profile-secret-token"
            obj.keys shouldContain "auth_token"
            obj["auth_token"]?.jsonPrimitive?.contentOrNull shouldBe null
            raw.contains("profile-secret-token") shouldBe false
        }
    }
})

private class FakeBotSecureTokenStore(vararg entries: Pair<String, String>) : BotSecureTokenStore {
    private val values = entries.toMap().toMutableMap()

    operator fun get(key: String): String? = values[key]

    override fun getString(key: String): String? = values[key]

    override fun putString(key: String, value: String) {
        values[key] = value
    }

    override fun remove(key: String) {
        values.remove(key)
    }
}
