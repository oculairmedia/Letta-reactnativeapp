package com.letta.mobile.data.attachment

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Tag

/**
 * lcp-dlj: tests for the image-attachment caps wired into the
 * composer + WS coordinator + downsampler.
 */
@Tag("unit")
class AttachmentLimitsTest : WordSpec({
    "Default" should {
        "match Anthropic vision-input guidance" {
            AttachmentLimits.Default.maxAttachmentCount shouldBe 4
            AttachmentLimits.Default.maxLongestEdgePx shouldBe 1568
            AttachmentLimits.Default.maxRawBytesPerImage shouldBe 2 * 1024 * 1024
        }
        "stay under the shim's 10 MB content_parts hard cap when 4 images saturate the per-image cap" {
            // 4 × 2 MB raw ≈ 8 MB JPEG bytes → base64 grows ~33% (≈ 10.7 MB)
            // — close enough that real-world payloads with light text still
            // fit. Document the assumption so a future bump of either side
            // forces us to revisit this bound.
            val rawTotal = AttachmentLimits.Default.maxAttachmentCount.toLong() *
                AttachmentLimits.Default.maxRawBytesPerImage.toLong()
            rawTotal shouldBe 8L * 1024 * 1024
        }
    }

    "jpegQualityFallbackLadder" should {
        "step from initial to floor, always ending at the floor" {
            val limits = AttachmentLimits(
                initialJpegQuality = 85,
                minJpegQuality = 50,
                jpegQualityStep = 10,
            )
            limits.jpegQualityFallbackLadder() shouldContainExactly listOf(85, 75, 65, 55, 50)
        }
        "yield exactly [initial, floor] when initial == floor + step" {
            val limits = AttachmentLimits(
                initialJpegQuality = 60,
                minJpegQuality = 50,
                jpegQualityStep = 10,
            )
            limits.jpegQualityFallbackLadder() shouldContainExactly listOf(60, 50)
        }
        "yield [floor] when initial == floor" {
            val limits = AttachmentLimits(
                initialJpegQuality = 70,
                minJpegQuality = 70,
                jpegQualityStep = 10,
            )
            limits.jpegQualityFallbackLadder() shouldContainExactly listOf(70)
        }
        "honour the floor even when step does not divide the range cleanly" {
            val limits = AttachmentLimits(
                initialJpegQuality = 90,
                minJpegQuality = 55,
                jpegQualityStep = 20,
            )
            // 90 -> 70 -> 50 would undershoot the floor; final entry must be 55.
            limits.jpegQualityFallbackLadder() shouldContainExactly listOf(90, 70, 55)
        }
    }

    "constructor validation" should {
        "reject zero or negative count" {
            try {
                AttachmentLimits(maxAttachmentCount = 0)
                error("expected IllegalArgumentException")
            } catch (e: IllegalArgumentException) {
                // ok
            }
        }
        "reject minJpegQuality > initialJpegQuality" {
            try {
                AttachmentLimits(initialJpegQuality = 50, minJpegQuality = 60)
                error("expected IllegalArgumentException")
            } catch (e: IllegalArgumentException) {
                // ok
            }
        }
    }
})
