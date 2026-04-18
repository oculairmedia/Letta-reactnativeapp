package com.letta.mobile.performance

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.metrics.performance.JankStats
import androidx.metrics.performance.PerformanceMetricsState
import com.letta.mobile.util.Telemetry
import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SentryLevel
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Debug-only performance monitor.
 *
 * Currently wires up:
 * - JankStats per Activity — frames >FRAME_BUDGET_MS trip a Sentry
 *   breadcrumb (category=performance, level=WARNING) plus a
 *   Telemetry.event(tag="Jank", name="frame", level=WARN).
 *
 * More subsystems (e.g. StrictMode) are added to this object incrementally
 * so that install() remains the single entry point from LettaApplication.
 *
 * The release variant ships a no-op twin in app/src/release/... with the
 * same public API so LettaApplication can call install() unconditionally.
 */
object DebugPerformanceMonitor {
    private const val FRAME_BUDGET_MS = 16L

    private val installed = AtomicBoolean(false)
    private val frameIndex = AtomicLong(0)
    private val jankStatsByActivity = Collections.synchronizedMap(WeakHashMap<Activity, JankStats>())

    fun install(application: Application) {
        if (!installed.compareAndSet(false, true)) {
            return
        }

        application.registerActivityLifecycleCallbacks(JankStatsLifecycleCallbacks())
        Telemetry.event(
            tag = "Perf",
            name = "debugInstrumentationEnabled",
            "jankStats" to true,
            "leakCanary" to true,
        )
    }

    private fun ensureJankStats(activity: Activity) {
        val existing = jankStatsByActivity[activity]
        if (existing != null) {
            existing.isTrackingEnabled = true
            return
        }

        val screenName = activity.javaClass.simpleName
        val metricsState = PerformanceMetricsState.getHolderForHierarchy(activity.window.decorView).state
        metricsState?.putState("screen", screenName)
        metricsState?.putState("activity", screenName)

        val frameListener = JankStats.OnFrameListener { frameData ->
            if (!frameData.isJank) {
                return@OnFrameListener
            }

            val durationMs = TimeUnit.NANOSECONDS.toMillis(frameData.frameDurationUiNanos)
            if (durationMs <= FRAME_BUDGET_MS) {
                return@OnFrameListener
            }

            val currentFrameIndex = frameIndex.incrementAndGet()
            val breadcrumb = Breadcrumb().apply {
                category = "performance"
                type = "default"
                level = SentryLevel.WARNING
                message = "Slow frame on $screenName"
                setData("screen", screenName)
                setData("durationMs", durationMs)
                setData("frameIndex", currentFrameIndex)
                setData("isJank", frameData.isJank)
            }
            Sentry.addBreadcrumb(breadcrumb)
            Telemetry.event(
                tag = "Jank",
                name = "frame",
                "screen" to screenName,
                "frameIndex" to currentFrameIndex,
                durationMs = durationMs,
                level = Telemetry.Level.WARN,
            )
        }
        val stats = JankStats.createAndTrack(activity.window, frameListener)
        jankStatsByActivity[activity] = stats
    }

    private class JankStatsLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            ensureJankStats(activity)
        }

        override fun onActivityStarted(activity: Activity) {
            ensureJankStats(activity)
        }

        override fun onActivityResumed(activity: Activity) {
            ensureJankStats(activity)
        }

        override fun onActivityPaused(activity: Activity) {
            jankStatsByActivity[activity]?.isTrackingEnabled = false
        }

        override fun onActivityStopped(activity: Activity) {
            jankStatsByActivity[activity]?.isTrackingEnabled = false
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

        override fun onActivityDestroyed(activity: Activity) {
            jankStatsByActivity.remove(activity)?.isTrackingEnabled = false
        }
    }
}
