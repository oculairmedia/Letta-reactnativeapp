package com.letta.mobile.ui.text

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.jupiter.api.Tag

@Tag("unit")
class RichInlineTextTest {

    @Test
    fun `prepare collapses whitespace across text boundaries`() {
        val prepared = prepareRichInlineItems(
            listOf(
                RichInlineItem.Text("  Ship  "),
                RichInlineItem.Text("   the   note  "),
            ),
        )

        assertEquals(
            listOf(PreparedRichInlineItem.Text("Ship the note")),
            prepared,
        )
    }

    @Test
    fun `prepare keeps atoms as unmerged inline items`() {
        val prepared = prepareRichInlineItems(
            listOf(
                RichInlineItem.Text("Ship "),
                RichInlineItem.Atom(
                    id = "mention-0",
                    text = "@maya",
                    kind = RichInlineAtomKind.Mention,
                ),
                RichInlineItem.Text("'s rich note"),
            ),
        )

        assertEquals(
            listOf(
                PreparedRichInlineItem.Text("Ship "),
                PreparedRichInlineItem.Atom(
                    id = "mention-0",
                    text = "@maya",
                    kind = RichInlineAtomKind.Mention,
                ),
                PreparedRichInlineItem.Text("'s rich note"),
            ),
            prepared,
        )
    }

    @Test
    fun `prepare drops blank atoms and trims paragraph boundary spaces`() {
        val prepared = prepareRichInlineItems(
            listOf(
                RichInlineItem.Text("\n  "),
                RichInlineItem.Atom(
                    id = "blank",
                    text = " ",
                    kind = RichInlineAtomKind.Chip,
                ),
                RichInlineItem.Text(" hello\t\t"),
            ),
        )

        assertEquals(
            listOf(PreparedRichInlineItem.Text("hello")),
            prepared,
        )
    }

    @Test
    fun `prepare preserves a single collapsed gap around atoms`() {
        val prepared = prepareRichInlineItems(
            listOf(
                RichInlineItem.Text("hello   "),
                RichInlineItem.Atom(
                    id = "math-0",
                    text = "x+1",
                    kind = RichInlineAtomKind.Math,
                ),
                RichInlineItem.Text("   world"),
            ),
        )

        assertEquals(
            listOf(
                PreparedRichInlineItem.Text("hello "),
                PreparedRichInlineItem.Atom(
                    id = "math-0",
                    text = "x+1",
                    kind = RichInlineAtomKind.Math,
                ),
                PreparedRichInlineItem.Text(" world"),
            ),
            prepared,
        )
    }
}
