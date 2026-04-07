package com.letta.mobile.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmojiDetectorTest {

    @Test
    fun `single emoji is emoji only`() {
        assertTrue(isEmojiOnly("😀"))
    }

    @Test
    fun `multiple emoji is emoji only`() {
        assertTrue(isEmojiOnly("😀😂🎉"))
    }

    @Test
    fun `emoji with spaces is emoji only`() {
        assertTrue(isEmojiOnly("😀 😂"))
    }

    @Test
    fun `text is not emoji only`() {
        assertFalse(isEmojiOnly("hello"))
    }

    @Test
    fun `emoji with text is not emoji only`() {
        assertFalse(isEmojiOnly("hello 😀"))
    }

    @Test
    fun `empty string is not emoji only`() {
        assertFalse(isEmojiOnly(""))
    }

    @Test
    fun `whitespace only is not emoji only`() {
        assertFalse(isEmojiOnly("   "))
    }

    @Test
    fun `emojiCount for single emoji`() {
        assertEquals(1, emojiCount("😀"))
    }

    @Test
    fun `emojiCount for three emoji`() {
        assertEquals(3, emojiCount("😀😂🎉"))
    }

    @Test
    fun `emojiCount for text returns zero`() {
        assertEquals(0, emojiCount("hello"))
    }

    @Test
    fun `emojiCount for empty returns zero`() {
        assertEquals(0, emojiCount(""))
    }
}
