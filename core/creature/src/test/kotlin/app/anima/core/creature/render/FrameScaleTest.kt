package app.anima.core.creature.render

import android.graphics.Bitmap
import app.anima.core.model.CreatureConcept
import app.anima.core.model.Mood
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * v1.1 (defect D6): eight bodies, eight different ideas of how big a creature
 * is. FOX_KIT filled its tile, SPROUT and MOTH hung small in the middle, and
 * because the body is assigned rather than chosen, whoever drew a small one
 * simply got a worse-looking phone.
 *
 * The fix has to be a contract, not eight hand-tuned multipliers that drift
 * apart again — so this measures the actual ink in a rendered frame and holds
 * every concept inside one band. It reads StillRender's real output rather
 * than the goldens, because the goldens capture a Compose node and every one
 * of them therefore measures the same box no matter what is inside it.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FrameScaleTest {
    private data class Extent(
        val widthFraction: Float,
        val heightFraction: Float,
        val centerYFraction: Float,
    )

    private fun measure(
        concept: CreatureConcept,
        mood: Mood = Mood.ALERT,
    ): Extent {
        val bmp =
            StillRender.tile(
                concept = concept,
                seed = SEED,
                mood = mood,
                batteryPercent = 80,
                charging = false,
                night = false,
                growth = 1f,
                sizePx = SIZE,
            )
        return inkExtent(bmp)
    }

    /**
     * Bounding box of pixels solid enough to read as body. The threshold
     * deliberately ignores the soft glow around the orb and the ember: a halo
     * is not silhouette, and counting it would let a concept "fill the frame"
     * with light while its body stayed tiny.
     */
    private fun inkExtent(bmp: Bitmap): Extent {
        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        var maxX = -1
        var maxY = -1
        val row = IntArray(bmp.width)
        for (y in 0 until bmp.height) {
            bmp.getPixels(row, 0, bmp.width, 0, y, bmp.width, 1)
            for (x in row.indices) {
                val alpha = (row[x] ushr 24) and 0xFF
                if (alpha < SOLID_ALPHA) continue
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }
        check(maxX >= 0) { "nothing was drawn" }
        val size = bmp.width.toFloat()
        return Extent(
            widthFraction = (maxX - minX + 1) / size,
            heightFraction = (maxY - minY + 1) / size,
            centerYFraction = ((minY + maxY) / 2f) / size,
        )
    }

    @Test
    fun `report every concept's extent`() {
        // Not an assertion — a printed table, so the numbers behind the bands
        // below are visible in the test output rather than folded into a
        // pass/fail. v1.1b: all four states, because the ALERT-only version of
        // this table is exactly why EMBER shipped half-sized when asleep.
        CreatureConcept.entries.forEach { c ->
            STATES.forEach { (slug, mood) ->
                val e = measure(c, mood)
                println(
                    "FRAME-SCALE ${c.name.padEnd(12)} ${slug.padEnd(14)} " +
                        "w=%.3f h=%.3f longest=%.3f cy=%.3f".format(
                            e.widthFraction,
                            e.heightFraction,
                            maxOf(e.widthFraction, e.heightFraction),
                            e.centerYFraction,
                        ),
                )
            }
        }
    }

    @Test
    fun `every concept fills a comparable share of its frame when alert`() {
        CreatureConcept.entries.forEach { c ->
            val e = measure(c)
            val longest = maxOf(e.widthFraction, e.heightFraction)
            assertWithMessage("$c alert fill").that(longest).isAtLeast(MIN_FILL)
            assertWithMessage("$c alert fill").that(longest).isAtMost(MAX_FILL)
        }
    }

    /**
     * v1.1b task 3. The ALERT-only version above is the whole reason EMBER
     * shipped at 0.506 of its frame when asleep while measuring 0.822 awake: the
     * widget and the wallpaper draw whichever state the phone is actually in, so
     * a body that only holds its size in one of four states is not calibrated.
     *
     * The floor is lower here than the ALERT floor on purpose. A curled sleeping
     * pose legitimately takes less room — FOX_KIT measures 0.693 asleep against
     * 0.818 awake and reads correctly on the contact sheet. 0.66 sits below that
     * and far above EMBER's 0.506, i.e. it is drawn from the measurements rather
     * than chosen for roundness.
     */
    @Test
    fun `every concept fills a comparable share of its frame in every state`() {
        CreatureConcept.entries.forEach { c ->
            STATES.forEach { (slug, mood) ->
                val e = measure(c, mood)
                val longest = maxOf(e.widthFraction, e.heightFraction)
                assertWithMessage("$c $slug fill").that(longest).isAtLeast(MIN_FILL_ANY_STATE)
                assertWithMessage("$c $slug fill").that(longest).isAtMost(MAX_FILL)
            }
        }
    }

    @Test
    fun `no concept is more than a third smaller than the largest, in any state`() {
        STATES.forEach { (slug, mood) ->
            val fills =
                CreatureConcept.entries.associateWith { c ->
                    val e = measure(c, mood)
                    maxOf(e.widthFraction, e.heightFraction)
                }
            val smallest = fills.values.min()
            val largest = fills.values.max()
            assertWithMessage(
                "$slug: smallest ${fills.minBy { it.value }.key} vs largest ${fills.maxBy { it.value }.key}",
            ).that(smallest / largest)
                .isAtLeast(MIN_RATIO)
        }
    }

    /**
     * The one that names EMBER's defect directly: not "small compared to other
     * bodies" but "small compared to itself". A body whose states disagree about
     * how big it is reads as a rig glitch, and on the home screen the owner has
     * nothing to compare it against except their memory of yesterday.
     *
     * 0.80 is the empty gap in the measurements: every other concept holds
     * 0.847–1.000 across its own four states, EMBER held 0.616.
     */
    @Test
    fun `no body changes size drastically between its own states`() {
        CreatureConcept.entries.forEach { c ->
            val fills =
                STATES.associate { (slug, mood) ->
                    val e = measure(c, mood)
                    slug to maxOf(e.widthFraction, e.heightFraction)
                }
            val ratio = fills.values.min() / fills.values.max()
            assertWithMessage("$c across its own states: $fills")
                .that(ratio)
                .isAtLeast(MIN_SELF_CONSISTENCY)
        }
    }

    @Test
    fun `every concept sits roughly on the same optical centre, in every state`() {
        // A body that fills the frame but sits high reads as badly cropped
        // next to one that sits centred — SPROUT drew its flower in the top
        // half and left the bottom empty. v1.1b: checked per state, because
        // EMBER's asleep ink sat at 0.673 while its awake ink sat at 0.473.
        CreatureConcept.entries.forEach { c ->
            STATES.forEach { (slug, mood) ->
                val e = measure(c, mood)
                assertWithMessage("$c $slug centre").that(e.centerYFraction).isAtLeast(0.5f - CENTER_TOLERANCE)
                assertWithMessage("$c $slug centre").that(e.centerYFraction).isAtMost(0.5f + CENTER_TOLERANCE)
            }
        }
    }

    private companion object {
        /**
         * v1.1b task 3: the four states the widget and the wallpaper can be
         * caught in — the same four the rig goldens and the contact sheet use,
         * so a number here can be checked against a picture there.
         */
        val STATES =
            listOf(
                "alert-day" to Mood.ALERT,
                "eating-charge" to Mood.EATING,
                "sleepy-low" to Mood.SLEEPY,
                "asleep-night" to Mood.ASLEEP,
            )

        const val SEED = 909_090L
        const val SIZE = 512
        const val SOLID_ALPHA = 128

        /** The band every body must land in when ALERT, longest dimension. */
        const val MIN_FILL = 0.70f
        const val MAX_FILL = 0.96f

        /**
         * The floor for any of the four states. Below the ALERT floor because a
         * curled sleeping pose legitimately shrinks (FOX_KIT: 0.693 asleep,
         * 0.818 awake); above EMBER's broken 0.506 by a wide margin.
         */
        const val MIN_FILL_ANY_STATE = 0.66f

        /**
         * Smallest of a body's own four states over its largest. Chosen from the
         * gap in the data: 0.847–1.000 for seven concepts, 0.616 for EMBER.
         */
        const val MIN_SELF_CONSISTENCY = 0.80f

        /** Smallest body may not be under two thirds of the largest. */
        const val MIN_RATIO = 0.66f

        /** How far the ink's vertical midpoint may sit from the frame's. */
        const val CENTER_TOLERANCE = 0.08f
    }
}
