package app.anima.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import app.anima.core.testing.GoldenOptions
import app.anima.core.ui.components.GhostButton
import app.anima.core.ui.components.SectionCard
import app.anima.core.ui.components.SectionLabel
import app.anima.core.ui.theme.AnimaTheme
import app.anima.core.ui.theme.LocalAnimaColors
import app.anima.core.ui.theme.Season
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Phase 0.4 screenshot regression: the design system in BOTH themes.
 * Season pinned (the seasonal tint reads the wall clock); no animation, no
 * wall-clock text — bit-stable goldens in src/test/screenshots/ds/.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DesignSystemGoldenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `design system goldens - light and dark`() {
        compose.setContent {
            Column {
                listOf(false to "light", true to "dark").forEach { (dark, slug) ->
                    AnimaTheme(darkTheme = dark, seasonOverride = Season.SPRING) {
                        val colors = LocalAnimaColors.current
                        Column(
                            Modifier
                                .testTag("ds-$slug")
                                .width(360.dp)
                                .background(colors.background)
                                .padding(16.dp),
                        ) {
                            SectionLabel("A QUIET SECTION")
                            SectionCard {
                                Text("The creature lives here.", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "Body text explains itself in one calm line.",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                GhostButton("Do the gentle thing", onClick = {})
                            }
                            SectionCard(Modifier.fillMaxWidth()) {
                                SectionLabel("SECOND CARD")
                                Text("Dim text tone.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
        listOf("light", "dark").forEach { slug ->
            compose
                .onNodeWithTag("ds-$slug")
                .captureRoboImage(
                    "src/test/screenshots/ds/design-system-$slug.png",
                    roborazziOptions = GoldenOptions,
                )
        }
    }
}
