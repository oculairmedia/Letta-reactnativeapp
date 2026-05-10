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

    "BotConfig serialization" should {
        "document current plaintext remote and API server token persistence" {
            val raw = json.encodeToString(
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
            )
            val obj = json.parseToJsonElement(raw).jsonArray.single().jsonObject

            obj.keys shouldContain "remote_token"
            obj.keys shouldContain "api_server_token"
            obj["remote_token"]?.jsonPrimitive?.contentOrNull shouldBe "remote-secret-token"
            obj["api_server_token"]?.jsonPrimitive?.contentOrNull shouldBe "api-server-secret-token"
        }
    }

    "BotServerProfile serialization" should {
        "document current plaintext auth token persistence" {
            val raw = json.encodeToString(
                listOf(
                    BotServerProfile(
                        id = "profile-1",
                        displayName = "Self hosted",
                        baseUrl = "https://server.example.test",
                        authToken = "profile-secret-token",
                    ),
                ),
            )
            val obj = json.parseToJsonElement(raw).jsonArray.single().jsonObject

            obj.keys shouldContain "auth_token"
            obj["auth_token"]?.jsonPrimitive?.contentOrNull shouldBe "profile-secret-token"
        }
    }
})
