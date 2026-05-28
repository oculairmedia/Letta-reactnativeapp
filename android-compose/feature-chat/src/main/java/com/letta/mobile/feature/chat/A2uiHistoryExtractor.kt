package com.letta.mobile.feature.chat

import com.letta.mobile.data.a2ui.A2uiMessage
import com.letta.mobile.data.a2ui.A2uiProtocolJson
import com.letta.mobile.data.a2ui.decodeA2uiMessages
import com.letta.mobile.data.model.UiMessage

internal data class A2uiHistoryExtraction(
    val messages: List<UiMessage>,
    val a2uiMessages: List<A2uiMessage>,
)

internal object A2uiHistoryExtractor {
    fun extract(messages: List<UiMessage>): A2uiHistoryExtraction {
        if (messages.none { it.content.contains(A2uiJsonOpenTag, ignoreCase = true) }) {
            return A2uiHistoryExtraction(messages = messages, a2uiMessages = emptyList())
        }

        val decoded = mutableListOf<A2uiMessage>()
        val cleaned = messages.map { message ->
            val nextContent = A2uiJsonBlockRegex.replace(message.content) { match ->
                val rawJson = match.groupValues[1].trim()
                runCatching {
                    decoded += decodeA2uiMessages(
                        A2uiProtocolJson.Default,
                        A2uiProtocolJson.Default.parseToJsonElement(rawJson),
                    )
                }.onFailure { error ->
                    android.util.Log.w("A2UI", "Failed to parse historical a2ui-json block", error)
                }
                ""
            }.trim()
            if (nextContent == message.content) message else message.copy(content = nextContent)
        }
        return A2uiHistoryExtraction(messages = cleaned, a2uiMessages = decoded)
    }

    private const val A2uiJsonOpenTag = "<a2ui-json>"
    private val A2uiJsonBlockRegex = Regex(
        pattern = """<a2ui-json>(.*?)</a2ui-json>""",
        options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
}
