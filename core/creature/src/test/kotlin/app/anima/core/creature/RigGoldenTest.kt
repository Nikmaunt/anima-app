package app.anima.core.creature

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import app.anima.core.creature.engine.CreatureEngine
import app.anima.core.creature.render.RenderContext
import app.anima.core.creature.render.Renderers
import app.anima.core.model.CreatureConcept
import app.anima.core.model.CreatureGenome
import app.anima.core.model.Mood
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Phase 0.4 screenshot regression: the rig itself, 8 concepts x 4 key body
 * states, rendered EXACTLY like the widget snapshot does it — a seeded
 * engine, reduced motion, one 16 ms tick, one deterministic frame. No frame
 * loop, no wall clock, no randomness outside the fixed seed, so the PNGs are
 * bit-stable. Goldens live in src/test/screenshots/rig/; `gradlew
 * :core:creature:verifyRoborazziDebug` is part of the DoD.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RigGoldenTest {
    @get:Rule
    val compose = createComposeRule()

    private data class RigState(
        val slug: String,
        val mood: Mood,
        val night: Boolean,
        val batteryPercent: Int,
        val charging: Boolean,
    )

    private val states =
        listOf(
            RigState("alert-day", Mood.ALERT, night = false, batteryPercent = 80, charging = false),
            RigState("eating-charge", Mood.EATING, night = false, batteryPercent = 65, charging = true),
            RigState("sleepy-low", Mood.SLEEPY, night = false, batteryPercent = 15, charging = false),
            RigState("asleep-night", Mood.ASLEEP, night = true, batteryPercent = 50, charging = false),
        )

    @Test
    fun `rig goldens - every concept in every key state`() {
        val current = mutableStateOf<ImageBitmap?>(null)
        compose.setContent {
            current.value?.let { Image(it, contentDescription = null, modifier = Modifier.testTag(TAG)) }
        }
        CreatureConcept.entries.forEach { concept ->
            states.forEach { state ->
                current.value = renderTile(concept, state).asImageBitmap()
                compose.waitForIdle()
                compose
                    .onNodeWithTag(TAG)
                    .captureRoboImage("src/test/screenshots/rig/${concept.name.lowercase()}-${state.slug}.png")
            }
        }
    }

    /** WidgetSnapshot.render's exact recipe, kept in the rig's own module. */
    private fun renderTile(
        concept: CreatureConcept,
        state: RigState,
    ): Bitmap {
        val genome = CreatureGenome.from(SEED)
        val engine = CreatureEngine(SEED, genome)
        engine.setReducedMotion(true)
        engine.setMood(state.mood)
        engine.onResume()
        engine.advance(ONE_TICK_NANOS)

        val image = ImageBitmap(SIZE_PX, SIZE_PX)
        val context =
            RenderContext().apply {
                pose = engine.pose
                this.genome = genome
                mood = state.mood
                timeSeconds = 0f
                night = state.night
                seed = SEED
                batteryPercent = state.batteryPercent
                charging = state.charging
                growth = GROWTH
            }
        val renderer = Renderers.forConcept(concept)
        CanvasDrawScope().draw(
            Density(1f),
            LayoutDirection.Ltr,
            Canvas(image),
            Size(SIZE_PX.toFloat(), SIZE_PX.toFloat()),
        ) {
            with(renderer) { render(context) }
        }
        return image.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false)
    }

    private companion object {
        const val TAG = "rig-golden"
        const val SEED = 42L
        const val SIZE_PX = 512
        const val GROWTH = 0.3f
        const val ONE_TICK_NANOS = 16_000_000L
    }
}
