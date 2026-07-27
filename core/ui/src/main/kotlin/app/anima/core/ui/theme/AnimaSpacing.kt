package app.anima.core.ui.theme

import androidx.compose.ui.unit.dp

/**
 * One spacing ladder with deliberate gaps, so "about that much" is not
 * expressible. Grouping is done with air (docs/design-system-v11.md §3):
 * `xl` between meaning groups, `s` inside one. If a value wants to be 20,
 * the grouping is wrong, not the ladder.
 */
object AnimaSpacing {
    /** Inside a line. */
    val xs = 4.dp

    /** Between related elements. */
    val s = 8.dp

    /** Inside a group; screen gutter on compact widths. */
    val m = 16.dp

    /** Inside a plate. */
    val l = 24.dp

    /** Between meaning groups. This is the one that replaces card borders. */
    val xl = 32.dp

    /** Around the hero. */
    val xxl = 56.dp

    /** Screen gutter past 600dp. */
    val gutterWide = 32.dp
}

/**
 * Corner radii, three distinguishable steps. Before v1.1 everything shared
 * one value, which is why a chip, a field and a card read as relatives.
 */
object AnimaRadius {
    /** Chips, input fields. */
    val s = 12.dp

    /** Plates. */
    val l = 22.dp
}
