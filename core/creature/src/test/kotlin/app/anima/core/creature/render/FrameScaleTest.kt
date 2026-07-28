package app.anima.core.creature.render

import android.graphics.Bitmap
import app.anima.core.model.CreatureConcept
import app.anima.core.model.Mood
import com.google.common.truth.Truth.assertThat
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
        // Not an assertion — a printed table, so the numbers behind the band
        // below are visible in the test output rather than folded into a
        // pass/fail.
        CreatureConcept.entries.forEach { c ->
            val e = measure(c)
            println(
                "FRAME-SCALE ${c.name.padEnd(12)} " +
                    "w=%.3f h=%.3f cy=%.3f".format(e.widthFraction, e.heightFraction, e.centerYFraction),
            )
        }
    }

    @Test
    fun `every concept fills a comparable share of its frame`() {
        CreatureConcept.entries.forEach { c ->
            val e = measure(c)
            val longest = maxOf(e.widthFraction, e.heightFraction)
            assertThat(longest).isAtLeast(MIN_FILL)
            assertThat(longest).isAtMost(MAX_FILL)
        }
    }

    @Test
    fun `no concept is more than a third smaller than the largest`() {
        val fills =
            CreatureConcept.entries.associateWith { c ->
                val e = measure(c)
                maxOf(e.widthFraction, e.heightFraction)
            }
        val smallest = fills.values.min()
        val largest = fills.values.max()
        assertThat(smallest / largest).isAtLeast(MIN_RATIO)
    }

    @Test
    fun `every concept sits roughly on the same optical centre`() {
        // A body that fills the frame but sits high reads as badly cropped
        // next to one that sits centred — SPROUT drew its flower in the top
        // half and left the bottom empty.
        CreatureConcept.entries.forEach { c ->
            val e = measure(c)
            assertThat(e.centerYFraction).isAtLeast(0.5f - CENTER_TOLERANCE)
            assertThat(e.centerYFraction).isAtMost(0.5f + CENTER_TOLERANCE)
        }
    }

    private companion object {
        const val SEED = 909_090L
        const val SIZE = 512
        const val SOLID_ALPHA = 128

        /** The band every body must land in, longest dimension. */
        const val MIN_FILL = 0.70f
        const val MAX_FILL = 0.96f

        /** Smallest body may not be under two thirds of the largest. */
        const val MIN_RATIO = 0.66f

        /** How far the ink's vertical midpoint may sit from the frame's. */
        const val CENTER_TOLERANCE = 0.08f
    }
}
