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
 * expanded threshold, stacked otherwise, chat landing in the right pane.
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

    private val state =
        HomeUiState(
            creatureName = "Iskra",
            mindStatus = MindStatus.READY,
            messages =
                listOf(
                    ChatMessage("m1", ChatRole.USER, "How was your day?", 1_000L),
                    ChatMessage(
                        "m2",
                        ChatRole.CREATURE,
                        "Soft and slow. I listened to the battery hum.",
                        2_000L,
                    ),
                ),
        )

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
                    cards = {},
                    chat = { modifier ->
                        ChatPanel(
                            state = state,
                            onSend = {},
                            onTyping = {},
                            onRequestDownload = {},
                            onOpenMind = {},
                            onRegenerate = {},
                            onRememberThis = {},
                            modifier = modifier,
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
        compose.onRoot().captureRoboImage("src/test/screenshots/home/adaptive-expanded.png")
    }

    @Test
    @Config(qualifiers = PHONE_DEVICE)
    fun `compact stacked`() {
        compose.setContent { Scaffold(expanded = false) }
        compose.onRoot().captureRoboImage("src/test/screenshots/home/adaptive-compact.png")
    }
}
