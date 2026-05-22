package com.letta.mobile.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.jupiter.api.Tag

@Tag("unit")
class HctColorHarmonizerTest {

    @Test
    fun `zero strength returns original color`() {
        val stateColor = Color.hsl(5f, 0.7f, 0.45f, alpha = 0.72f)

        val harmonized = HctColorHarmonizer.harmonize(
            stateColor = stateColor,
            seedColor = Color.hsl(170f, 0.8f, 0.5f),
            strength = 0f,
        )

        assertEquals(stateColor, harmonized)
    }

    @Test
    fun `hue moves along shortest path across zero degree boundary`() {
        val harmonized = HctColorHarmonizer.harmonize(
            stateColor = Color.hsl(350f, 0.7f, 0.5f),
            seedColor = Color.hsl(10f, 0.7f, 0.5f),
            strength = 0.5f,
        )

        val hue = harmonized.toHslColor().hue
        assertTrue("Expected hue to move toward 0° across the short path, was $hue", hue in 354f..356f)
    }

    @Test
    fun `harmonization preserves state tone and alpha`() {
        val stateColor = Color.hsl(20f, 0.62f, 0.41f, alpha = 0.65f)

        val harmonized = HctColorHarmonizer.harmonize(
            stateColor = stateColor,
            seedColor = Color.hsl(180f, 0.9f, 0.8f),
            strength = 0.25f,
        )

        val originalTone = contrastTone(stateColor)
        val harmonizedTone = contrastTone(harmonized)
        assertEquals(originalTone, harmonizedTone, ToneTolerance)
        assertEquals(stateColor.alpha, harmonized.alpha, AlphaTolerance)
    }

    @Test
    fun `container harmonization keeps accessible contrast with content color`() {
        val contentColor = Color(0xFF222222)
        val harmonized = HctColorHarmonizer.harmonizeContainer(
            containerColor = Color(0xFF333333),
            seedColor = Color(0xFF00897B),
            contentColor = contentColor,
            minContrastRatio = 4.5,
        )

        assertTrue(contrastRatio(contentColor, harmonized) >= 4.5)
    }

    private fun contrastRatio(foreground: Color, background: Color): Double =
        ColorUtils.calculateContrast(foreground.toOpaqueArgb(), background.toOpaqueArgb())

    private fun contrastTone(color: Color): Double =
        com.google.android.material.color.utilities.Hct.fromInt(color.toOpaqueArgb()).tone

    private fun Color.toOpaqueArgb(): Int = ColorUtils.setAlphaComponent(toArgb(), OpaqueAlpha)

    private companion object {
        const val ToneTolerance = 1.0
        const val AlphaTolerance = 0.001f
        const val OpaqueAlpha = 255
    }
}
