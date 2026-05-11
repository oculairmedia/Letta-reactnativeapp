package com.letta.mobile.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates the startup-only ART profile used by AGP/R8 for DEX layout
 * optimization. This deliberately covers only the launcher startup path;
 * broader scroll/chat CUJs stay in [BaselineProfileGenerator] so the primary
 * DEX pages are reserved for classes needed before first interactive draw.
 *
 * Run with:
 *
 *     ./gradlew :app:generateBenchmarkBaselineProfile
 *
 * The Startup Profile is emitted as:
 *   :app/src/benchmark/generated/baselineProfiles/startup-prof.txt
 *
 * See letta-mobile-o7ob.2.2.
 */
@RunWith(AndroidJUnit4::class)
class StartupProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(
        packageName = ProfileTarget.targetPackageName(),
        includeInStartupProfile = true,
    ) {
        ProfileTarget.grantOptionalRuntimePermissions(device)
        ProfileTarget.startActivityAndWait(this)
        device.wait(
            Until.hasObject(By.pkg(ProfileTarget.targetPackageName()).depth(0)),
            STARTUP_WAIT_MS,
        )
        ProfileTarget.waitForProfileFlush(device)
    }

    private companion object {
        const val STARTUP_WAIT_MS = ProfileTarget.STARTUP_WAIT_MS
    }
}
