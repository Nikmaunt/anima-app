package app.anima.feature.widget

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.anima.core.model.CreatureConcept
import app.anima.core.model.Mood
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Widget snapshot render on a real device image (GMD). The render path is
 * pure software drawing (CanvasDrawScope → Bitmap), so the ATD emulator's
 * lack of hardware rendering is irrelevant — what this buys over Robolectric
 * is the real Skia backend and real Bitmap allocation limits.
 */
@RunWith(AndroidJUnit4::class)
class WidgetSnapshotDeviceTest {
    private val vitals = WidgetSnapshot.Vitals(batteryPercent = 80, charging = false)

    @Test
    fun every_concept_renders_a_non_blank_512px_frame() {
        CreatureConcept.entries.forEach { concept ->
            val bitmap =
                WidgetSnapshot.render(
                    concept = concept,
                    seed = 42L,
                    mood = Mood.ALERT,
                    vitals = vitals,
                    night = false,
                    growth = 0.5f,
                )
            assertThat(bitmap.width).isEqualTo(WidgetSnapshot.SIZE_PX)
            assertThat(bitmap.height).isEqualTo(WidgetSnapshot.SIZE_PX)
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val drawn = pixels.count { (it ushr 24) != 0 }
            assertThat(drawn).isGreaterThan(0)
            bitmap.recycle()
        }
    }

    @Test
    fun mood_derivation_matches_the_vitals() {
        assertThat(WidgetSnapshot.moodFor(WidgetSnapshot.Vitals(50, charging = true), night = false))
            .isEqualTo(Mood.EATING)
        assertThat(WidgetSnapshot.moodFor(WidgetSnapshot.Vitals(50, charging = false), night = true))
            .isEqualTo(Mood.ASLEEP)
        assertThat(WidgetSnapshot.moodFor(WidgetSnapshot.Vitals(10, charging = false), night = false))
            .isEqualTo(Mood.SLEEPY)
        assertThat(WidgetSnapshot.moodFor(WidgetSnapshot.Vitals(80, charging = false), night = false))
            .isEqualTo(Mood.ALERT)
    }
}
