package app.anima.feature.widget

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.anima.core.model.CreatureConcept
import app.anima.core.model.Mood
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * v1.1c task 6.1 — a real widget frame, on a device image, written to a file.
 *
 * `WidgetSnapshotDeviceTest` next door already proves each concept renders
 * something non-blank. What it does not do is leave the picture anywhere, and
 * "non-blank" is a very low bar for a surface that sits on the home screen
 * between the launcher icons. This writes the frames out so they can be opened
 * and judged, which is the only test that catches "it renders, and it looks
 * wrong".
 */
@RunWith(AndroidJUnit4::class)
class WidgetFrameDumpTest {
    private fun outDir(): File {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        return File(context.getExternalFilesDir(null), "widget-frames").apply { mkdirs() }
    }

    @Test
    fun every_body_gets_a_widget_frame_on_disk() {
        val states =
            listOf(
                Triple("day-alert", Mood.ALERT, false),
                Triple("night-asleep", Mood.ASLEEP, true),
                Triple("charging", Mood.EATING, false),
            )
        CreatureConcept.entries.forEach { concept ->
            states.forEach { (slug, mood, night) ->
                val bitmap =
                    WidgetSnapshot.render(
                        concept = concept,
                        seed = SEED,
                        mood = mood,
                        vitals =
                            WidgetSnapshot.Vitals(
                                batteryPercent = if (mood == Mood.SLEEPY) 15 else 80,
                                charging = mood == Mood.EATING,
                            ),
                        night = night,
                        growth = 0.5f,
                    )
                val opaque = countOpaque(bitmap)
                assertWithMessage("$concept $slug is an empty tile")
                    .that(opaque)
                    .isGreaterThan(MIN_PAINTED_PIXELS)
                // The widget tile must stay transparent outside the body: it sits
                // on the launcher's own wallpaper and a filled tile is a card.
                assertWithMessage("$concept $slug filled its whole tile — the widget is not a card")
                    .that(opaque)
                    .isLessThan(bitmap.width * bitmap.height)
                File(outDir(), "widget-${concept.name.lowercase()}-$slug.png")
                    .outputStream()
                    .use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
            }
        }
        println("WIDGET-DUMP ${outDir().absolutePath}")
    }

    private fun countOpaque(bitmap: Bitmap): Int {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return pixels.count { (it ushr 24) > OPAQUE_ENOUGH }
    }

    private companion object {
        const val SEED = 909_090L
        const val MIN_PAINTED_PIXELS = 3_000
        const val OPAQUE_ENOUGH = 128
    }
}
