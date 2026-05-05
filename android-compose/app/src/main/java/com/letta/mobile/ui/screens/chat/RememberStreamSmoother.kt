package com.letta.mobile.ui.screens.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Composable-side wrapper around [StreamingDisplayTextSmoother].
 *
 * Returns a smoothed version of [rawText] that reveals characters at a
 * steady cadence while [isStreaming] is true. When the stream ends, the
 * remaining tail drains at an accelerated rate and the function returns
 * the full text.
 *
 * Usage:
 * ```
 * val smoothed = rememberSmoothedStreamingText(
 *     rawText = message.content,
 *     isStreaming = isStreaming,
 * )
 * Text(text = smoothed)
 * ```
 *
 * The frame loop only runs while text is being revealed. Once the smoother
 * is fully caught up and the stream is closed, the loop suspends — no
 * wasted frames at rest.
 */
@Composable
fun rememberSmoothedStreamingText(
    rawText: String,
    isStreaming: Boolean,
): String {
    val smoother = remember { StreamingDisplayTextSmoother() }
    var displayedText by remember { mutableStateOf("") }
    // Track last update to avoid redundant updates that reset the clock
    var lastKnownText by remember { mutableStateOf("") }

    // letta-mobile-flk2 (fix): use rememberUpdatedState to read the latest
    // rawText/isStreaming without restarting the frame loop on every chunk.
    // The prior LaunchedEffect(rawText, isStreaming) key caused cancel+restart
    // on every chunk arrival, which reset the smoother's lastStepMs clock and
    // caused visible flicker (oscillation between stale and ahead text).
    // This matches the pattern used in StreamingMarkdownText.kt (revision 3).
    val latestRawText by rememberUpdatedState(rawText)
    val latestIsStreaming by rememberUpdatedState(isStreaming)

    val nowMs = { System.nanoTime() / 1_000_000L }

    // Only update target when text actually changes — calling updateTarget
    // on every frame resets the clock (lastStepMs = nowMs), causing advance=1
    // even if many frames have passed. The smoother needs cumulative time
    // to reveal characters at the configured rate.
    if (latestRawText != lastKnownText) {
        smoother.updateTarget(latestRawText, latestIsStreaming, nowMs())
        lastKnownText = latestRawText
    }

    // Frame loop: runs continuously while text is being revealed.
    // Uses Unit key so the effect never restarts — it reads the latest
    // rawText/isStreaming via rememberUpdatedState on each tick.
    // The loop naturally suspends once isFullyRevealed && !latestIsStreaming.
    LaunchedEffect(Unit) {
        while (isActive && !(smoother.isFullyRevealed && !latestIsStreaming)) {
            displayedText = smoother.step(nowMs())
            delay(FRAME_INTERVAL_MS)
        }
        // Final step to ensure we don't leave a partial reveal.
        displayedText = smoother.step(nowMs())
    }

    return displayedText
}

/** Target frame interval — 16 ms ≈ 60 fps. */
private const val FRAME_INTERVAL_MS = 16L
