package com.letta.mobile.ui.text

import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.jupiter.api.Tag

@Tag("unit")
class ChatTextGeometryCacheTest {

    @Test
    fun `cache reuses geometry for identical prepared text key`() {
        val cache = ChatTextGeometryCache(maxEntries = 4)
        val key = geometryKey("hello world", widthPx = 240)
        var producerCalls = 0

        val first = cache.getOrPut(key) {
            producerCalls++
            geometry(lineCount = 1)
        }
        val second = cache.getOrPut(key) {
            producerCalls++
            geometry(lineCount = 99)
        }

        assertSame(first, second)
        assertEquals(1, producerCalls)
        assertEquals(1, second.lineCount)
    }

    @Test
    fun `cache evicts least recently used geometry`() {
        val cache = ChatTextGeometryCache(maxEntries = 2)
        val first = geometryKey("first", widthPx = 160)
        val second = geometryKey("second", widthPx = 160)
        val third = geometryKey("third", widthPx = 160)

        cache.getOrPut(first) { geometry(lineCount = 1) }
        cache.getOrPut(second) { geometry(lineCount = 2) }
        cache.get(first)
        cache.getOrPut(third) { geometry(lineCount = 3) }

        assertEquals(2, cache.size())
        assertEquals(1, cache.get(first)?.lineCount)
        assertEquals(null, cache.get(second))
        assertEquals(3, cache.get(third)?.lineCount)
    }

    @Test
    fun `content key includes enough text identity to guard hash and length reuse`() {
        val base = "a".repeat(120)
        val differentSuffix = "a".repeat(119) + "b"

        assertNotEquals(base.chatTextContentKey(), differentSuffix.chatTextContentKey())
    }

    @Test
    fun `geometry key changes for width density font scale direction style and mode`() {
        val base = geometryKey("same text", widthPx = 240)

        assertNotEquals(base, base.copy(widthPx = 320))
        assertNotEquals(base, base.copy(density = 3f))
        assertNotEquals(base, base.copy(fontScale = 1.2f))
        assertNotEquals(base, base.copy(layoutDirection = LayoutDirection.Rtl))
        assertNotEquals(base, base.copy(styleFingerprint = 42))
        assertNotEquals(base, base.copy(mode = ChatTextLayoutMode.Code))
    }

    private fun geometryKey(text: String, widthPx: Int): ChatTextGeometryKey =
        ChatTextGeometryKey(
            content = text.chatTextContentKey(),
            widthPx = widthPx,
            density = 2f,
            fontScale = 1f,
            layoutDirection = LayoutDirection.Ltr,
            styleFingerprint = 7,
            mode = ChatTextLayoutMode.Plain,
            softWrap = true,
            maxLines = Int.MAX_VALUE,
        )

    private fun geometry(lineCount: Int): ChatTextGeometry =
        ChatTextGeometry(
            lineCount = lineCount,
            heightPx = lineCount * 20,
            maxLineWidthPx = 100,
            visibleLineEndOffsets = IntArray(lineCount) { it + 1 },
        )
}
