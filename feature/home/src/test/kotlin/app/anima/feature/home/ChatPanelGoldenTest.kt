package app.anima.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.LayoutDirection
import app.anima.core.model.ChatMessage
import app.anima.core.model.ChatRole
import app.anima.core.model.MindStatus
import app.anima.core.ui.theme.AnimaTheme
import app.anima.core.ui.theme.LocalAnimaColors
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
 * v0.5 DoD (ADR-014): locale goldens + pseudolocale sweeps for the chat
 * panel — the densest localized surface of Home (empty-state / starters /
 * regenerate / input hint / send a11y). ChatPanel is stateless
 * (HomeUiState + callbacks), so no Hilt VM is involved. Message texts are
 * deliberately fixed user content: only chrome differs between locales.
 * Pseudolocale shots are NOT goldens — plain PNGs into build/pseudoloc/.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ChatPanelGoldenTest {
    private companion object {
        /** Pixel-6-class portrait; the 320x470dp Robolectric default is no real phone. */
        const val DEVICE = "w411dp-h914dp-420dpi"
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
            starters = listOf(HomeStarter.FedTimes(3), HomeStarter.RememberToday),
        )

    private fun render(rtl: Boolean = false) {
        compose.setContent {
            // Library test manifests carry no android:supportsRtl (it lives
            // in :app), so mirroring must be forced at the composition local.
            val direction = if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                AnimaTheme(darkTheme = false, seasonOverride = Season.SPRING) {
                    val colors = LocalAnimaColors.current
                    Box(Modifier.fillMaxSize().background(colors.background)) {
                        ChatPanel(
                            state = state,
                            onSend = {},
                            onTyping = {},
                            onRequestDownload = {},
                            onOpenMind = {},
                            onRegenerate = {},
                            onRememberThis = {},
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }

    private fun golden(slug: String) {
        render()
        compose.onRoot().captureRoboImage("src/test/screenshots/home/$slug.png")
    }

    /**
     * Plain PNG dump: task type is forced to Record so the file is written
     * on every test run and never becomes a compare/verify baseline.
     */
    private fun pseudoloc(
        slug: String,
        rtl: Boolean = false,
    ) {
        render(rtl)
        compose.onRoot().captureRoboImage(
            filePath = "build/pseudoloc/$slug.png",
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record),
        )
    }

    @Test
    @Config(qualifiers = "+ru-$DEVICE")
    fun `chat ru`() = golden("chat-ru")

    @Test
    @Config(qualifiers = "+ja-$DEVICE")
    fun `chat ja`() = golden("chat-ja")

    @Test
    @Config(qualifiers = "+en-rXA-$DEVICE")
    fun `pseudo expansion chat`() = pseudoloc("home-chat-enXA")

    @Test
    @Config(qualifiers = "+ar-rXB-ldrtl-$DEVICE")
    fun `pseudo rtl chat`() = pseudoloc("home-chat-arXB", rtl = true)
}
