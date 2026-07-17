package app.anima.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates baseline + startup profiles for the app's cold-start path:
 * splash → home → the creature's first frames. Run on a connected device:
 * `gradlew :app:generateBaselineProfile` (see ADR-006).
 */
@RunWith(AndroidJUnit4::class)
class StartupProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() =
        rule.collect(
            packageName = "app.anima",
            includeInStartupProfile = true,
        ) {
            pressHome()
            startActivityAndWait()
            // Let the frame loop settle a few seconds of creature animation —
            // the renderers are the hot path worth pre-compiling.
            Thread.sleep(3_000)
        }
}
