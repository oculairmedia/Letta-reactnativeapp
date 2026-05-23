package com.letta.mobile.ui.haptics

import android.annotation.SuppressLint
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * Centralized semantic haptic vocabulary for Letta UI.
 *
 * Prefer the platform [View.performHapticFeedback] constants because it returns
 * whether the device accepted the effect. Compose's haptic bridge is still the
 * final fallback for old call sites, but routing modern devices through [View]
 * preserves the richer Pixel haptic vocabulary instead of collapsing everything
 * into the same generic Compose pulse.
 */
object HapticEffects {
    fun confirm(haptic: HapticFeedback, view: View? = null, enabled: Boolean = true) {
        performExpressive(
            enabled = enabled,
            haptic = haptic,
            view = view,
            composeType = HapticFeedbackType.Confirm,
            platformType = { HapticFeedbackConstants.CONFIRM },
            fallbackPlatformType = HapticFeedbackConstants.CONTEXT_CLICK,
        )
    }

    fun reject(haptic: HapticFeedback, view: View? = null, enabled: Boolean = true) {
        performExpressive(
            enabled = enabled,
            haptic = haptic,
            view = view,
            composeType = HapticFeedbackType.Reject,
            platformType = { HapticFeedbackConstants.REJECT },
            fallbackPlatformType = HapticFeedbackConstants.LONG_PRESS,
        )
    }

    fun toggleOn(haptic: HapticFeedback, view: View? = null, enabled: Boolean = true) {
        performExpressive(
            enabled = enabled,
            haptic = haptic,
            view = view,
            composeType = HapticFeedbackType.ToggleOn,
            platformType = { HapticFeedbackConstants.TOGGLE_ON },
            fallbackPlatformType = HapticFeedbackConstants.CLOCK_TICK,
        )
    }

    fun toggleOff(haptic: HapticFeedback, view: View? = null, enabled: Boolean = true) {
        performExpressive(
            enabled = enabled,
            haptic = haptic,
            view = view,
            composeType = HapticFeedbackType.ToggleOff,
            platformType = { HapticFeedbackConstants.TOGGLE_OFF },
            fallbackPlatformType = HapticFeedbackConstants.CLOCK_TICK,
        )
    }

    fun segmentTick(haptic: HapticFeedback, view: View? = null, enabled: Boolean = true) {
        performExpressive(
            enabled = enabled,
            haptic = haptic,
            view = view,
            composeType = HapticFeedbackType.SegmentTick,
            platformType = { HapticFeedbackConstants.SEGMENT_TICK },
            fallbackPlatformType = HapticFeedbackConstants.CLOCK_TICK,
        )
    }

    fun gestureThreshold(haptic: HapticFeedback, view: View? = null, enabled: Boolean = true) {
        performExpressive(
            enabled = enabled,
            haptic = haptic,
            view = view,
            composeType = HapticFeedbackType.GestureThresholdActivate,
            platformType = { HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE },
            fallbackPlatformType = HapticFeedbackConstants.LONG_PRESS,
        )
    }

    fun contextClick(haptic: HapticFeedback, view: View? = null, enabled: Boolean = true) {
        if (!enabled) return
        if (performViewHaptic(view, HapticFeedbackConstants.CONTEXT_CLICK)) return
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun longPress(haptic: HapticFeedback, view: View? = null, enabled: Boolean = true) {
        if (!enabled) return
        if (performViewHaptic(view, HapticFeedbackConstants.LONG_PRESS)) return
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    @SuppressLint("NewApi")
    private fun performExpressive(
        enabled: Boolean,
        haptic: HapticFeedback,
        view: View?,
        composeType: HapticFeedbackType,
        platformType: () -> Int,
        fallbackPlatformType: Int,
    ) {
        if (!enabled) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && performViewHaptic(view, platformType())) {
            return
        }
        if (performViewHaptic(view, fallbackPlatformType)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            haptic.performHapticFeedback(composeType)
            return
        }
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    private fun performViewHaptic(view: View?, feedbackConstant: Int): Boolean =
        view?.performHapticFeedback(feedbackConstant) == true
}
