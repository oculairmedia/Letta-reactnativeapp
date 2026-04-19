package com.letta.mobile.designsystem

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.letta.mobile.ui.theme.scaledBy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.jupiter.api.Tag

/**
 * Regression tests for chat font scaling via [scaledBy].
 *
 * These tests ensure that the pinch-to-zoom scaling function correctly applies
 * font size and line height multipliers to TextStyle objects, and that edge
 * cases (identity factor, unspecified sizes, boundary values) are handled.
 */
@Tag("unit")
class ChatFontScaleTest {

    @Test
    fun `scaledBy with factor 1 returns same instance`() {
        val style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)
        val result = style.scaledBy(1f)
        assertSame("Factor 1f should return the exact same object", style, result)
    }

    @Test
    fun `scaledBy scales fontSize correctly`() {
        val style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)
        val result = style.scaledBy(1.5f)
        assertSpEquals(21.sp, result.fontSize)
    }

    @Test
    fun `scaledBy scales lineHeight correctly`() {
        val style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)
        val result = style.scaledBy(1.5f)
        assertSpEquals(30.sp, result.lineHeight)
    }

    @Test
    fun `scaledBy with unspecified fontSize preserves it`() {
        val style = TextStyle(lineHeight = 20.sp)
        val result = style.scaledBy(1.5f)
        assertFalse("Unspecified fontSize should remain unspecified", result.fontSize.isSpecified)
        assertSpEquals(30.sp, result.lineHeight)
    }

    @Test
    fun `scaledBy with unspecified lineHeight preserves it`() {
        val style = TextStyle(fontSize = 14.sp)
        val result = style.scaledBy(1.5f)
        assertSpEquals(21.sp, result.fontSize)
        assertFalse("Unspecified lineHeight should remain unspecified", result.lineHeight.isSpecified)
    }

    @Test
    fun `scaledBy with both unspecified preserves both`() {
        val style = TextStyle()
        val result = style.scaledBy(1.5f)
        assertFalse("Unspecified fontSize should remain unspecified", result.fontSize.isSpecified)
        assertFalse("Unspecified lineHeight should remain unspecified", result.lineHeight.isSpecified)
    }

    @Test
    fun `scaledBy at minimum pinch boundary 0_7`() {
        val style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)
        val result = style.scaledBy(0.7f)
        assertSpEquals(9.8.sp, result.fontSize)
        assertSpEquals(14.0.sp, result.lineHeight)
    }

    @Test
    fun `scaledBy at maximum pinch boundary 1_6`() {
        val style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)
        val result = style.scaledBy(1.6f)
        assertSpEquals(22.4.sp, result.fontSize)
        assertSpEquals(32.0.sp, result.lineHeight)
    }

    @Test
    fun `scaledBy preserves other TextStyle properties`() {
        val style = TextStyle(
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.4.sp,
        )
        val result = style.scaledBy(1.5f)
        assertTrue("letterSpacing should be preserved", result.letterSpacing.isSpecified)
        assertSpEquals(0.4.sp, result.letterSpacing)
    }

    private fun assertSpEquals(expected: TextUnit, actual: TextUnit, tolerance: Float = 0.01f) {
        assertTrue("Expected specified TextUnit", expected.isSpecified)
        assertTrue("Actual should be specified", actual.isSpecified)
        assertEquals(
            "Expected ${expected.value}sp but got ${actual.value}sp",
            expected.value,
            actual.value,
            tolerance,
        )
    }
}
