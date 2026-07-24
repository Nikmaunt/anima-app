package app.anima.core.data.prefs

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Security-relevant preference DEFAULTS as tests (DoD v0.7): a fresh install
 * must ship with FLAG_SECURE on (screenshots refused) and with the shipped
 * mind runtime (LiteRT-LM developer flag OFF, ADR-020).
 */
@RunWith(RobolectricTestRunner::class)
class AnimaPrefsDefaultsTest {
    private val prefs = AnimaPrefs(ApplicationProvider.getApplicationContext<Context>())

    @Test
    fun `litertlm engine flag defaults to OFF`() =
        runTest {
            assertThat(prefs.litertlmEngine().first()).isFalse()
        }

    @Test
    fun `soul screenshots default to refused`() =
        runTest {
            assertThat(prefs.soulScreenshotsAllowed().first()).isFalse()
        }

    @Test
    fun `switch implementation mirrors the flag`() =
        runTest {
            val switch = PrefsMindEngineSwitch(prefs)
            assertThat(switch.altLocalEngine.first()).isFalse()
            prefs.setLitertlmEngine(true)
            assertThat(switch.altLocalEngine.first()).isTrue()
        }
}
