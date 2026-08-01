package app.anima.feature.wallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.anima.core.model.CreatureConcept
import app.anima.core.model.Mood
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * v1.1c task 6 — the live wallpaper, rendered on a device image and written out
 * so a person can open it.
 *
 * The wallpaper has been in the product since v0.4 and, by the previous run's
 * own admission, "was never placed on a home screen" in eleven runs. Its
 * drawing code changed substantially in v1.1 (`StillRender`), in v1.1b
 * (grounding) and again in v1.1c (the moth), and none of those changes was ever
 * looked at on this surface.
 *
 * This is a dumper, not an assertion suite — the assertions it does make are
 * the two that can be made without eyes:
 *
 *  * something was drawn (a frame of pure background is a defect, unless it is
 *    the identity-unknown frame, where it is the *point*);
 *  * the identity-unknown frame contains exactly the background and nothing
 *    else, which is task 6.4.
 *
 * Everything else about these files is for a human. Pull them with:
 * `adb shell run-as ... ` is not available for a test APK, so they are written
 * to the app's external files dir and pulled with plain `adb pull`.
 */
@RunWith(AndroidJUnit4::class)
class WallpaperFrameDumpTest {
    private val width = 1080
    private val height = 2340

    private fun outDir(): File {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        return File(context.getExternalFilesDir(null), "wallpaper-frames").apply { mkdirs() }
    }

    private fun render(
        concept: CreatureConcept?,
        seed: Long?,
        mood: Mood,
        night: Boolean,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        WallpaperFrame.draw(
            canvas = Canvas(bitmap),
            concept = concept,
            seed = seed,
            mood = mood,
            batteryPercent = if (mood == Mood.SLEEPY) 15 else 80,
            charging = mood == Mood.EATING,
            night = night,
            paletteShiftDeg = 0f,
        )
        return bitmap
    }

    private fun save(
        bitmap: Bitmap,
        name: String,
    ) {
        File(outDir(), name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Task 6.3: all eight, day and night, not only whichever one this phone is. */
    @Test
    fun every_body_gets_a_wallpaper_frame_on_disk() {
        val states =
            listOf(
                Triple("day-alert", Mood.ALERT, false),
                Triple("night-asleep", Mood.ASLEEP, true),
            )
        CreatureConcept.entries.forEach { concept ->
            states.forEach { (slug, mood, night) ->
                val bitmap = render(concept, SEED, mood, night)
                val background = if (night) WallpaperFrame.NIGHT_BG else WallpaperFrame.DAY_BG
                val painted = countNonBackground(bitmap, background)
                assertWithMessage("$concept $slug drew nothing but background")
                    .that(painted)
                    .isGreaterThan(MIN_PAINTED_PIXELS)
                save(bitmap, "wallpaper-${concept.name.lowercase()}-$slug.png")
                bitmap.recycle()
            }
        }
        println("WALLPAPER-DUMP ${outDir().absolutePath}")
    }

    /**
     * Task 6.4: the path where identity cannot be read. This is a full-screen
     * surface on the home screen, so the rule is nobody rather than somebody
     * else — and "nobody" has to be provable, not assumed.
     */
    @Test
    fun with_no_identity_the_wallpaper_is_background_and_nothing_else() {
        listOf(true, false).forEach { night ->
            val bitmap = render(concept = null, seed = null, mood = Mood.ALERT, night = night)
            val background = if (night) WallpaperFrame.NIGHT_BG else WallpaperFrame.DAY_BG
            assertWithMessage("an unknown identity must not put a body on the home screen")
                .that(countNonBackground(bitmap, background))
                .isEqualTo(0)
            save(bitmap, "wallpaper-no-identity-${if (night) "night" else "day"}.png")
            bitmap.recycle()
        }

        // And a seed without a body, and a body without a seed: half an identity
        // is not an identity, and both halves used to have their own fallback.
        listOf(
            CreatureConcept.FOX_KIT to null,
            null to SEED,
        ).forEach { (concept, seed) ->
            val bitmap = render(concept, seed, Mood.ALERT, night = false)
            assertThat(countNonBackground(bitmap, WallpaperFrame.DAY_BG)).isEqualTo(0)
            bitmap.recycle()
        }
    }

    private fun countNonBackground(
        bitmap: Bitmap,
        background: Int,
    ): Int {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return pixels.count { it != background }
    }

    private companion object {
        const val SEED = 909_090L

        /** A stray antialiased pixel is not a creature; a body is thousands. */
        const val MIN_PAINTED_PIXELS = 10_000
    }
}
