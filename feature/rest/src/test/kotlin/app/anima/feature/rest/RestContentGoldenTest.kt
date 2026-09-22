package app.anima.feature.rest

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import app.anima.core.model.CreatureConcept
import app.anima.core.model.RestPhase
import app.anima.core.testing.GoldenOptions
import app.anima.core.ui.theme.AnimaTheme
import app.anima.core.ui.theme.Season
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Stateless-first pays off: the whole rest screen goldens without a VM.
 * Fixed clock values, pinned season, frame loop off inside CreatureSurface
 * (the surface renders its initial pose; no ticking during composition
 * because the LaunchedEffect clock lives in the stateful wrapper).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RestContentGoldenTest {
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

    private fun golden(
        slug: String,
        dark: Boolean,
        phase: RestPhase,
    ) {
        compose.setContent {
            AnimaTheme(darkTheme = dark, seasonOverride = Season.SPRING) {
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
        compose.onRoot().captureRoboImage(
            "src/test/screenshots/rest/$slug.png",
            roborazziOptions = GoldenOptions,
        )
    }

    @Test
    fun `pick panel light`() = golden("pick-light", dark = false, phase = RestPhase.Idle)

    @Test
    fun `pick panel dark`() = golden("pick-dark", dark = true, phase = RestPhase.Idle)

    @Test
    fun `running dark`() =
        golden(
            "running-dark",
            dark = true,
            phase = RestPhase.Running(plannedMin = 10, accumulatedMs = 0L, anchorMs = 0L),
        )

    @Test
    fun `completed light`() = golden("completed-light", dark = false, phase = RestPhase.Completed(10))
}
