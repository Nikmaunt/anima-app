package app.anima.core.creature.render

import android.graphics.Bitmap
import app.anima.core.model.CreatureConcept
import app.anima.core.model.Mood
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Not a check — a dumper. Writes the raw `StillRender` output for every
 * concept in every key state into `build/contact/`, so the contact sheet
 * (v1.1 task 5.3) can be composited over real backdrops.
 *
 * The rig goldens cannot be used for this: they capture a Compose node inside
 * a Robolectric window, so every one of them measures the same box no matter
 * what is inside it, and none of them is the transparent frame the widget and
 * the wallpaper actually draw.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ContactSheetDumpTest {
    @Test
    fun `dump every concept in every key state`() {
        val out = File("build/contact").apply { mkdirs() }
        val states =
            listOf(
                Triple("alert-day", Mood.ALERT, false),
                Triple("eating-charge", Mood.EATING, false),
                Triple("sleepy-low", Mood.SLEEPY, false),
                Triple("asleep-night", Mood.ASLEEP, true),
            )
        CreatureConcept.entries.forEach { concept ->
            states.forEach { (slug, mood, night) ->
                val bmp =
                    StillRender.tile(
                        concept = concept,
                        seed = SEED,
                        mood = mood,
                        batteryPercent = if (mood == Mood.SLEEPY) 15 else 80,
                        charging = mood == Mood.EATING,
                        night = night,
                        growth = 1f,
                        sizePx = SIZE,
                    )
                File(out, "${concept.name.lowercase()}-$slug.png").outputStream().use {
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        }
        println("CONTACT-DUMP ${out.absolutePath}")
    }

    private companion object {
        const val SEED = 909_090L
        const val SIZE = 512
    }
}
