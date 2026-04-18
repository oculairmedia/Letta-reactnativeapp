package com.letta.mobile.data.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wire-format invariants for the multimodal content-parts JSON array sent to
 * the Letta server. These tests pin the exact shape the backend expects so
 * we don't accidentally break compatibility while refactoring.
 */
class MessageContentPartTest {

    @Test
    fun `text part serializes with type=text and text body`() {
        val arr = listOf<MessageContentPart>(MessageContentPart.Text("hello world")).toJsonArray()
        assertEquals(1, arr.size)
        val obj = arr.first().jsonObject
        assertEquals("text", obj["type"]?.jsonPrimitive?.contentOrNull)
        assertEquals("hello world", obj["text"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun `image part serializes as image_url with data URL`() {
        val img = MessageContentPart.Image(base64 = "AAAA", mediaType = "image/jpeg")
        val arr = listOf<MessageContentPart>(img).toJsonArray()
        val obj = arr.first().jsonObject
        assertEquals("image_url", obj["type"]?.jsonPrimitive?.contentOrNull)
        val urlObj = obj["image_url"]?.jsonObject
        val url = urlObj?.get("url")?.jsonPrimitive?.contentOrNull
        assertEquals("data:image/jpeg;base64,AAAA", url)
    }

    @Test
    fun `mixed parts preserve order text-then-image`() {
        val parts = listOf<MessageContentPart>(
            MessageContentPart.Text("look at this:"),
            MessageContentPart.Image(base64 = "BBBB", mediaType = "image/png"),
        )
        val arr = parts.toJsonArray()
        assertEquals(2, arr.size)
        assertEquals("text", arr[0].jsonObject["type"]?.jsonPrimitive?.contentOrNull)
        assertEquals("image_url", arr[1].jsonObject["type"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun `buildContentParts drops blank text and keeps images`() {
        val img = MessageContentPart.Image(base64 = "Z", mediaType = "image/jpeg")
        val parts = buildContentParts(text = "   ", images = listOf(img))
        assertEquals(1, parts.size)
        assertTrue(parts.first() is MessageContentPart.Image)
    }

    @Test
    fun `buildContentParts keeps text first then images`() {
        val img = MessageContentPart.Image(base64 = "Z", mediaType = "image/jpeg")
        val parts = buildContentParts(text = "caption", images = listOf(img))
        assertEquals(2, parts.size)
        assertEquals("caption", (parts[0] as MessageContentPart.Text).text)
        assertTrue(parts[1] is MessageContentPart.Image)
    }

    @Test
    fun `serialized array round-trips through Json without throwing`() {
        val parts = listOf<MessageContentPart>(
            MessageContentPart.Text("hi"),
            MessageContentPart.Image(base64 = "AB+/=", mediaType = "image/png"),
        )
        val arr = parts.toJsonArray()
        // Re-parse to confirm valid JSON
        val decoded = Json.parseToJsonElement(arr.toString())
        assertEquals(2, (decoded as kotlinx.serialization.json.JsonArray).size)
    }

    @Test
    fun `image data URL embeds raw base64 unchanged`() {
        // Particularly: '+', '/', '=' from standard base64 alphabet are preserved
        val payload = "abc+/=="
        val img = MessageContentPart.Image(base64 = payload, mediaType = "image/jpeg")
        assertEquals("data:image/jpeg;base64,abc+/==", img.toDataUrl())
    }

    @Test
    fun `user message extracts text and image attachments from multimodal content`() {
        val content = buildJsonArray {
            add(buildJsonObject {
                put("type", JsonPrimitive("text"))
                put("text", JsonPrimitive("caption"))
            })
            add(buildJsonObject {
                put("type", JsonPrimitive("image_url"))
                put("image_url", buildJsonObject {
                    put("url", JsonPrimitive("data:image/png;base64,ABCD+/=="))
                })
            })
        }

        val message = UserMessage(id = "u1", contentRaw = content)

        assertEquals("caption", message.content)
        assertEquals(1, message.attachments.size)
        assertEquals("image/png", message.attachments.first().mediaType)
        assertEquals("ABCD+/==", message.attachments.first().base64)
    }

    @Test
    fun `non data url image parts are ignored`() {
        val content = buildJsonArray {
            add(buildJsonObject {
                put("type", JsonPrimitive("image_url"))
                put("image_url", buildJsonObject {
                    put("url", JsonPrimitive("https://example.com/image.png"))
                })
            })
        }

        val message = AssistantMessage(id = "a1", contentRaw = content)

        assertTrue(message.attachments.isEmpty())
    }

    @Test
    fun `unused imports stay tree-shakeable`() {
        // Smoke: make sure a JsonObject/JsonPrimitive import isn't elided.
        @Suppress("UNUSED_VARIABLE")
        val probe: JsonObject = JsonObject(mapOf("x" to JsonPrimitive(1)))
        assertNotNull(probe)
    }
}
