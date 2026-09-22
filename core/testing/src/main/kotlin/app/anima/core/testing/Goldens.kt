package app.anima.core.testing

import com.dropbox.differ.SimpleImageComparator
import com.github.takahirom.roborazzi.RoborazziOptions

/**
 * The one comparison every golden in the repo is held to.
 *
 * Goldens are bit-stable per platform, not across them: the Linux CI runner
 * anti-aliases glyph edges up to 2/255 per channel differently from the
 * Windows machine that records them (per-pixel distance ≤ 0.0136, measured on
 * every golden that differed; Roborazzi's default limit is 0.007). 0.015 admits
 * exactly that and nothing a layout, colour or missing element would produce.
 * Every pixel must still pass it: this limits how far a pixel may move, never
 * how many may. Do not raise it — a diff above it is a real change.
 */
val GoldenOptions =
    RoborazziOptions(
        compareOptions =
            RoborazziOptions.CompareOptions(
                imageComparator = SimpleImageComparator(maxDistance = 0.015F),
            ),
    )
