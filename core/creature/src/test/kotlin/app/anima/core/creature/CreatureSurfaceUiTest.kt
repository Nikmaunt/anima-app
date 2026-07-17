package app.anima.core.creature

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import app.anima.core.model.BodySignals
import app.anima.core.model.BodyState
import app.anima.core.model.ChargeKind
import app.anima.core.model.CreatureConcept
import app.anima.core.model.CreatureGenome
import app.anima.core.model.Mood
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Compose UI tests for the creature's key states (v0.1 debt). Robolectric on
 * the JVM: verifies every concept composes, the frame loop starts, and every
 * mood plus reduced-motion renders without crashing. Pixel truth stays with
 * the S24 manual checklist; these tests pin composition and state plumbing.
 */
@RunWith(RobolectricTestRunner::class)
class CreatureSurfaceUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `every concept composes and survives a mood sweep in normal motion`() {
        val controllers = mutableListOf<CreatureController>()
        compose.setContent {
            CreatureConcept.entries.forEach { concept ->
                val controller = CreatureController(concept, seed = 42L, genome = CreatureGenome.from(42L))
                controllers += controller
                CreatureSurface(
                    controller = controller,
                    night = false,
                    modifier = Modifier.size(120.dp).testTag("creature-${concept.name}"),
                    interactive = false,
                    reducedMotionOverride = false,
                )
            }
        }
        CreatureConcept.entries.forEach { concept ->
            compose.onNodeWithTag("creature-${concept.name}").assertExists()
        }
        Mood.entries.forEach { mood ->
            controllers.forEach { it.setBodyState(bodyState(mood)) }
            compose.mainClock.advanceTimeBy(64)
            compose.waitForIdle()
        }
    }

    @Test
    fun `reduced motion still composes every mood including celebration`() {
        lateinit var controller: CreatureController
        compose.setContent {
            controller = rememberCreature(CreatureConcept.SPROUT, seed = 7L)
            CreatureSurface(
                controller = controller,
                night = true,
                modifier = Modifier.size(160.dp).testTag("creature"),
                interactive = false,
                reducedMotionOverride = true,
            )
        }
        compose.onNodeWithTag("creature").assertExists()
        Mood.entries.forEach { mood ->
            controller.setBodyState(bodyState(mood))
            compose.mainClock.advanceTimeBy(300)
            compose.waitForIdle()
        }
        controller.onCelebrate()
        controller.onStartle()
        compose.mainClock.advanceTimeBy(300)
        compose.waitForIdle()
    }

    @Test
    fun `growth and charging state changes render without recreation`() {
        lateinit var controller: CreatureController
        compose.setContent {
            controller = rememberCreature(CreatureConcept.SPIRIT_ORB, seed = 3L)
            CreatureSurface(
                controller = controller,
                night = false,
                modifier = Modifier.size(120.dp).testTag("creature"),
                interactive = false,
                reducedMotionOverride = false,
            )
        }
        listOf(0f, 0.5f, 1f).forEach { growth ->
            controller.setGrowth(growth)
            controller.setBodyState(bodyState(Mood.EATING, charging = true))
            compose.mainClock.advanceTimeBy(48)
            compose.waitForIdle()
        }
        compose.onNodeWithTag("creature").assertExists()
    }

    private fun bodyState(
        mood: Mood,
        charging: Boolean = false,
    ): BodyState =
        BodyState(
            signals =
                BodySignals.Resting.copy(
                    charging = charging,
                    chargeKind = if (charging) ChargeKind.AC else ChargeKind.NONE,
                    batteryPercent = if (mood == Mood.SLEEPY) 12 else 80,
                ),
            mood = mood,
        )
}
