package app.anima.feature.wallpaper

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WallpaperBudgetTest {
    @Test
    fun `invisible never draws whatever the reason`() {
        assertThat(
            WallpaperBudget.allowsDraw(WallpaperBudget.Reason.SURFACE, visible = false, 0L, null),
        ).isFalse()
        assertThat(
            WallpaperBudget.allowsDraw(WallpaperBudget.Reason.STATE, visible = false, 0L, null),
        ).isFalse()
    }

    @Test
    fun `surface reasons always draw while visible`() {
        assertThat(
            WallpaperBudget.allowsDraw(WallpaperBudget.Reason.SURFACE, visible = true, 0L, 0L),
        ).isTrue()
    }

    @Test
    fun `state draws are debounced to one per interval`() {
        val first =
            WallpaperBudget.allowsDraw(WallpaperBudget.Reason.STATE, visible = true, 1_000L, null)
        assertThat(first).isTrue()
        val tooSoon =
            WallpaperBudget.allowsDraw(WallpaperBudget.Reason.STATE, visible = true, 2_000L, 1_000L)
        assertThat(tooSoon).isFalse()
        val afterInterval =
            WallpaperBudget.allowsDraw(
                WallpaperBudget.Reason.STATE,
                visible = true,
                1_000L + WallpaperBudget.MIN_STATE_REDRAW_INTERVAL_MS,
                1_000L,
            )
        assertThat(afterInterval).isTrue()
    }
}
