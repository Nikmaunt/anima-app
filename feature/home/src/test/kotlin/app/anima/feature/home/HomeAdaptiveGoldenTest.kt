package app.anima.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import app.anima.core.model.ChatMessage
import app.anima.core.model.ChatRole
import app.anima.core.model.MindStatus
import app.anima.core.testing.GoldenOptions
import app.anima.core.ui.theme.AnimaTheme
import app.anima.core.ui.theme.LocalAnimaColors
import app.anima.core.ui.theme.Season
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * v0.6 tablets/folds: the adaptive scaffold's two branches as goldens. The
 * creature slot is a placeholder box (CreatureSurface runs an endless frame
 * loop — not goldenable); what these pin is the LAYOUT: pane split at the
 * expanded threshold, stacked otherwise.
 *
 * v1.1: there is no chat pane to pin any more. The scaffold's second slot
 * holds notices and the way out; chat moved to its own route behind an
 * off-by-default toggle, which is why the sample HomeUiState this test used
 * to build a ChatPanel from is gone.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeAdaptiveGoldenTest {
    private companion object {
        /** Unfolded fold / small tablet landscape: ≥840dp wide. */
        const val EXPANDED_DEVICE = "w1280dp-h800dp-land-320dpi"

        /** The phone rig used by every other home golden. */
        const val PHONE_DEVICE = "w411dp-h914dp-420dpi"
    }

    @get:Rule
    val compose = createComposeRule()

    @Composable
    private fun Scaffold(expanded: Boolean) {
        AnimaTheme(darkTheme = false, seasonOverride = Season.SPRING) {
            val colors = LocalAnimaColors.current
            Box(Modifier.fillMaxSize().background(colors.background)) {
                HomeAdaptiveScaffold(
                    expanded = expanded,
                    headerRow = {
                        Text(
                            "Iskra · 12 days together",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(20.dp),
                        )
                    },
                    creature = { modifier ->
                        Box(modifier.padding(20.dp).background(colors.surfaceHigh))
                    },
                    cards = {
                        // v1.1: the second pane holds notices, not a chat
                        // thread. What used to be here was the whole reason
                        // the creature was not the hero of its own screen.
                        Text(
                            "Soft and slow. I listened to the battery hum.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(20.dp),
                        )
                    },
                    navRow = {
                        Text(
                            "Rest · Soul · Diary · Settings",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(20.dp),
                        )
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    @Test
    @Config(qualifiers = EXPANDED_DEVICE)
    fun `expanded two-pane`() {
        compose.setContent { Scaffold(expanded = true) }
        compose.onRoot().captureRoboImage(
            "src/test/screenshots/home/adaptive-expanded.png",
            roborazziOptions = GoldenOptions,
        )
    }

    @Test
    @Config(qualifiers = PHONE_DEVICE)
    fun `compact stacked`() {
        compose.setContent { Scaffold(expanded = false) }
        compose.onRoot().captureRoboImage(
            "src/test/screenshots/home/adaptive-compact.png",
            roborazziOptions = GoldenOptions,
        )
    }
}
