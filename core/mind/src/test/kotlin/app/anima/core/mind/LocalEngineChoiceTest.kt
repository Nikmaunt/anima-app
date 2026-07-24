package app.anima.core.mind

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * ADR-020 routing law: the alt runtime serves the local tier only when the
 * developer flag is ON and the debug-only binding exists. In release the
 * binding is absent, so every row with altPresent=false is the release
 * guarantee: the flag alone can never switch the engine.
 */
class LocalEngineChoiceTest {
    @Test
    fun `default OFF routes to the shipped engine even when alt exists`() {
        assertThat(LocalEngineChoice.useAlt(flagOn = false, altPresent = true)).isFalse()
    }

    @Test
    fun `flag ON without a binding - release build - stays on the shipped engine`() {
        assertThat(LocalEngineChoice.useAlt(flagOn = true, altPresent = false)).isFalse()
    }

    @Test
    fun `flag ON with the debug binding routes to the alt engine`() {
        assertThat(LocalEngineChoice.useAlt(flagOn = true, altPresent = true)).isTrue()
    }

    @Test
    fun `default OFF without a binding stays local`() {
        assertThat(LocalEngineChoice.useAlt(flagOn = false, altPresent = false)).isFalse()
    }
}
