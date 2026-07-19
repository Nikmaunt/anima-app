package app.anima.feature.rest

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.LayoutDirection
import app.anima.core.model.CreatureConcept
import app.anima.core.model.RestPhase
import app.anima.core.ui.theme.AnimaTheme
import app.anima.core.ui.theme.Season
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * v0.5 DoD (ADR-014): locale goldens + pseudolocale sweeps for the rest
 * screen. RU/JA go through the Roborazzi golden flow (recorded baselines in
 * src/test/screenshots/rest). en-XA (expansion) and ar-XB (RTL) are NOT
 * goldens — pseudolocale text is generator noise, a pixel baseline would
 * churn — so they are written straight to build/pseudoloc/ for eyeballing.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LocaleGoldenTest {
    private companion object {
        /**
         * Pixel-6-class portrait. The Robolectric default device is 320x470dp
         * — so short that even the EN pick panel overflows it (no real phone
         * is that small), which would make every clipping verdict noise.
         */
        const val DEVICE = "w411dp-h914dp-420dpi"
    }

    @get:Rule
    val compose = createComposeRule()

    private val ui =
        RestUiState(
            concept = CreatureConcept.SPIRIT_ORB,
            seed = 42L,
            creatureName = "Iskra",
            totalSessions = 7,
            totalQuietMinutes = 95,
            weekSessions = 3,
        )

    private fun render(
        phase: RestPhase,
        rtl: Boolean = false,
    ) {
        compose.setContent {
            // The merged manifest of a library-module Robolectric test app has
            // no android:supportsRtl (that flag lives in :app), so the view
            // tree never mirrors on its own — force the composition local,
            // exactly what the real app gets on an RTL device.
            val direction = if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                content(phase)
            }
        }
    }

    @Suppress("ktlint:standard:function-naming")
    @androidx.compose.runtime.Composable
    private fun content(phase: RestPhase) {
        AnimaTheme(darkTheme = false, seasonOverride = Season.SPRING) {
            RestContent(
                ui = ui,
                phase = phase,
                nowMs = 90_000L,
                charging = false,
                keepScreenOn = false,
                cameBackFromWait = false,
                onPick = {},
                onKeepScreenOn = {},
                onDone = {},
                onBack = {},
                frameLoop = false,
            )
        }
    }

    private fun golden(
        slug: String,
        phase: RestPhase,
    ) {
        render(phase)
        compose.onRoot().captureRoboImage("src/test/screenshots/rest/$slug.png")
    }

    /**
     * Plain PNG dump: task type is forced to Record so the file is written
     * on every test run and never becomes a compare/verify baseline.
     */
    private fun pseudoloc(
        slug: String,
        phase: RestPhase,
        rtl: Boolean = false,
    ) {
        render(phase, rtl)
        compose.onRoot().captureRoboImage(
            filePath = "build/pseudoloc/$slug.png",
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record),
        )
    }

    // -- RU / JA goldens ---------------------------------------------------

    @Test
    @Config(qualifiers = "+ru-$DEVICE")
    fun `pick panel ru`() = golden("pick-ru", RestPhase.Idle)

    @Test
    @Config(qualifiers = "+ru-$DEVICE")
    fun `completed ru`() = golden("completed-ru", RestPhase.Completed(10))

    @Test
    @Config(qualifiers = "+ja-$DEVICE")
    fun `pick panel ja`() = golden("pick-ja", RestPhase.Idle)

    @Test
    @Config(qualifiers = "+ja-$DEVICE")
    fun `completed ja`() = golden("completed-ja", RestPhase.Completed(10))

    // -- Pseudolocale sweeps (manual visual check, not goldens) ------------

    @Test
    @Config(qualifiers = "+en-rXA-$DEVICE")
    fun `pseudo expansion pick`() = pseudoloc("rest-pick-enXA", RestPhase.Idle)

    @Test
    @Config(qualifiers = "+en-rXA-$DEVICE")
    fun `pseudo expansion completed`() = pseudoloc("rest-completed-enXA", RestPhase.Completed(10))

    @Test
    @Config(qualifiers = "+ar-rXB-ldrtl-$DEVICE")
    fun `pseudo rtl pick`() = pseudoloc("rest-pick-arXB", RestPhase.Idle, rtl = true)

    @Test
    @Config(qualifiers = "+ar-rXB-ldrtl-$DEVICE")
    fun `pseudo rtl completed`() = pseudoloc("rest-completed-arXB", RestPhase.Completed(10), rtl = true)
}
