package app.anima.feature.home

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import app.anima.core.ui.theme.AnimaTheme
import app.anima.core.ui.theme.Season
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * v1.1c task 5.5 — the `experimentalChat` toggle must not open onto nothing.
 *
 * The toggle is off by default and, when it is on, it adds a door to a screen
 * whose whole purpose depends on a model that may not be installed. Three
 * answers would have been wrong: an error page (this is not an error), an empty
 * box (the v1.1b defect class, in a different costume), and a live text field
 * that accepts what you type and never replies.
 *
 * The defined behaviour, pinned here: the creature says what the situation is,
 * and there is exactly one control, which leads to the screen that fixes it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h914dp-420dpi")
class ChatDeadEndTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `with no mind, chat says so and offers the one step that changes it`() {
        var openedMind = 0
        compose.setContent {
            AnimaTheme(darkTheme = true, seasonOverride = Season.SPRING) {
                ChatNoMind(onOpenMind = { openedMind++ })
            }
        }

        compose.onNodeWithTag("chat.noMind").assertIsDisplayed()
        compose.onNodeWithTag("chat.openMind").assertIsDisplayed().assertHasClickAction()

        compose.onNodeWithTag("chat.openMind").performClick()
        assertWithMessage("the one control on the screen has to lead somewhere")
            .that(openedMind)
            .isEqualTo(1)
    }

    /**
     * The negative half, and the one that matters: there is no way to type into
     * a conversation that cannot answer. `ChatPanel`'s input carries this tag,
     * and it must not be composed on this branch.
     */
    @Test
    fun `with no mind there is nothing to type into`() {
        compose.setContent {
            AnimaTheme(darkTheme = true, seasonOverride = Season.SPRING) {
                ChatNoMind(onOpenMind = {})
            }
        }
        compose.onNodeWithTag("chat.input").assertDoesNotExist()
        compose.onNodeWithTag("chat.send").assertDoesNotExist()
    }
}
