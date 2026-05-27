package com.letta.mobile.ui.text

internal sealed interface RichInlineItem {
    data class Text(val text: String) : RichInlineItem

    data class Atom(
        val id: String,
        val text: String,
        val kind: RichInlineAtomKind,
    ) : RichInlineItem
}

internal enum class RichInlineAtomKind {
    Math,
    Mention,
    Chip,
    Code,
}

internal sealed interface PreparedRichInlineItem {
    data class Text(val text: String) : PreparedRichInlineItem

    data class Atom(
        val id: String,
        val text: String,
        val kind: RichInlineAtomKind,
    ) : PreparedRichInlineItem
}

internal fun prepareRichInlineItems(items: List<RichInlineItem>): List<PreparedRichInlineItem> {
    if (items.isEmpty()) return emptyList()

    val prepared = mutableListOf<PreparedRichInlineItem>()
    var pendingSpace = false
    var hasOutput = false
    val textBuffer = StringBuilder()

    fun flushText() {
        if (textBuffer.isNotEmpty()) {
            prepared.add(PreparedRichInlineItem.Text(textBuffer.toString()))
            textBuffer.clear()
        }
    }

    fun appendCollapsedSpaceIfNeeded() {
        if (pendingSpace && hasOutput) {
            textBuffer.append(' ')
            pendingSpace = false
        }
    }

    for (item in items) {
        when (item) {
            is RichInlineItem.Text -> {
                for (char in item.text) {
                    if (char.isWhitespace()) {
                        pendingSpace = hasOutput || textBuffer.isNotEmpty()
                    } else {
                        appendCollapsedSpaceIfNeeded()
                        textBuffer.append(char)
                        hasOutput = true
                    }
                }
            }
            is RichInlineItem.Atom -> {
                if (item.text.isBlank()) continue
                appendCollapsedSpaceIfNeeded()
                flushText()
                prepared.add(
                    PreparedRichInlineItem.Atom(
                        id = item.id,
                        text = item.text,
                        kind = item.kind,
                    ),
                )
                hasOutput = true
                pendingSpace = false
            }
        }
    }
    flushText()
    return prepared
}
