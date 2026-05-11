package com.letta.mobile.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates a baseline profile for letta-mobile by walking the hot user path:
 * cold launch, first composed list, scroll, optional chat drill-in, and timeline
 * scroll. Targets are skipped when absent so an unprimed benchmark device still
 * emits a valid startup/list profile.
 *
 * Run with:
 *
 *     ./gradlew :app:generateBenchmarkBaselineProfile
 *
 * The generated profile lands at:
 *   :app/src/benchmark/generated/baselineProfiles/baseline-prof.txt
 *
 * Startup-only DEX layout rules are generated separately by
 * [StartupProfileGenerator] into startup-prof.txt.
 *
 * See letta-mobile-o7ob.2.1.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = ProfileTarget.targetPackageName()) {
        ProfileTarget.grantOptionalRuntimePermissions(device)
        ProfileTarget.startActivityAndWait(this)

        device.wait(
            Until.hasObject(By.pkg(ProfileTarget.targetPackageName()).depth(0)),
            STARTUP_WAIT_MS,
        )

        val list = device.findObject(By.scrollable(true))
        if (list != null) {
            list.setGestureMargin(device.displayWidth / 5)
            repeat(SCROLL_PASSES) {
                list.fling(Direction.DOWN)
                device.waitForIdle(IDLE_MS)
            }
            list.fling(Direction.UP)
            device.waitForIdle(IDLE_MS)
        }

        val firstRow = device.findObject(By.clickable(true))
        if (firstRow != null) {
            firstRow.click()
            device.waitForIdle(IDLE_MS)

            val timeline = device.findObject(By.scrollable(true))
            if (timeline != null) {
                timeline.setGestureMargin(device.displayWidth / 5)
                repeat(SCROLL_PASSES) {
                    timeline.fling(Direction.UP)
                    device.waitForIdle(IDLE_MS)
                }
            }
            device.pressBack()
            device.waitForIdle(IDLE_MS)
        }

        ProfileTarget.waitForProfileFlush(device)
    }

    private companion object {
        const val STARTUP_WAIT_MS = ProfileTarget.STARTUP_WAIT_MS
        const val SCROLL_PASSES = 3
        const val IDLE_MS = 500L
    }
}
