package app.anima.feature.home

import android.content.res.Configuration
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * v1.1c task 2.2. `HomeAdaptiveGoldenTest` pins what each branch looks like; it
 * is handed `expanded` as a boolean and therefore says nothing about *when* the
 * split happens. This says when.
 *
 * The case that made it necessary is the fourth row: an unfolded Fold held in
 * portrait is 904 x 1114dp — past the 840dp threshold and taller than it is
 * wide. Under the width-only rule it got two tall narrow columns with the
 * creature in a bottom corner (`docs/design/v11/after-v11b/16-home-expanded-
 * 1080dp-portrait-BAD.png`), and folds are this product's home turf.
 */
class TwoPaneRuleTest {
    private fun configuration(
        widthDp: Int,
        heightDp: Int,
    ) = Configuration().apply {
        screenWidthDp = widthDp
        screenHeightDp = heightDp
    }

    @Test
    fun `the rule is wide AND landscape`() {
        val cases =
            listOf(
                Triple("phone portrait", 411 to 914, false),
                // A large phone on its side is 914 x 411dp: past the threshold
                // and wide. Two panes are RIGHT here and this row is not a
                // mistake — landscape is where vertical space is scarce and a
                // second column earns itself. `15-home-expanded-891dp-land.png`
                // is what it looks like, and it works.
                Triple("phone landscape", 914 to 411, true),
                Triple("unfolded fold, landscape", 1114 to 904, true),
                Triple("unfolded fold, PORTRAIT", 904 to 1114, false),
                Triple("tablet landscape", 1280 to 800, true),
                Triple("tablet portrait", 800 to 1280, false),
                Triple("exactly at the threshold, landscape", 840 to 700, true),
                Triple("one dp under the threshold, landscape", 839 to 700, false),
                Triple("square, at the threshold", 900 to 900, false),
            )
        cases.forEach { (name, size, expected) ->
            val (w, h) = size
            assertWithMessage("$name — ${w}x$h dp")
                .that(isTwoPane(configuration(w, h)))
                .isEqualTo(expected)
        }
    }
}
