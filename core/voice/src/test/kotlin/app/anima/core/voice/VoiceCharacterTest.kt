package app.anima.core.voice

import app.anima.core.model.CreatureConcept
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VoiceCharacterTest {
    @Test
    fun `every concept stays inside the TTS-safe range across personality corners`() {
        for (concept in CreatureConcept.entries) {
            for (warmth in floatArrayOf(0f, 0.5f, 1f)) {
                for (chattiness in floatArrayOf(0f, 0.5f, 1f)) {
                    val v = VoiceCharacter.of(concept, warmth, chattiness)
                    assertThat(v.pitch).isAtLeast(VoiceCharacter.MIN_AXIS)
                    assertThat(v.pitch).isAtMost(VoiceCharacter.MAX_AXIS)
                    assertThat(v.rate).isAtLeast(VoiceCharacter.MIN_AXIS)
                    assertThat(v.rate).isAtMost(VoiceCharacter.MAX_AXIS)
                }
            }
        }
    }

    @Test
    fun `all eight concepts sound distinct at the same personality`() {
        val characters = CreatureConcept.entries.map { VoiceCharacter.of(it, 0.5f, 0.5f) }
        assertThat(characters.toSet()).hasSize(CreatureConcept.entries.size)
        // Distinct on pitch alone — the axis single-voice locales rely on.
        assertThat(characters.map { it.pitch }.toSet()).hasSize(CreatureConcept.entries.size)
    }

    @Test
    fun `warmth raises pitch monotonically and leaves rate alone`() {
        for (concept in CreatureConcept.entries) {
            val steps = listOf(0f, 0.25f, 0.5f, 0.75f, 1f).map { VoiceCharacter.of(concept, it, 0.5f) }
            steps.zipWithNext().forEach { (lower, higher) ->
                assertThat(higher.pitch).isGreaterThan(lower.pitch)
                assertThat(higher.rate).isEqualTo(lower.rate)
            }
        }
    }

    @Test
    fun `chattiness raises rate monotonically and leaves pitch alone`() {
        for (concept in CreatureConcept.entries) {
            val steps = listOf(0f, 0.25f, 0.5f, 0.75f, 1f).map { VoiceCharacter.of(concept, 0.5f, it) }
            steps.zipWithNext().forEach { (lower, higher) ->
                assertThat(higher.rate).isGreaterThan(lower.rate)
                assertThat(higher.pitch).isEqualTo(lower.pitch)
            }
        }
    }

    @Test
    fun `mapping is deterministic`() {
        for (concept in CreatureConcept.entries) {
            assertThat(VoiceCharacter.of(concept, 0.3f, 0.7f))
                .isEqualTo(VoiceCharacter.of(concept, 0.3f, 0.7f))
        }
    }

    @Test
    fun `out-of-range personality clamps to the unit interval`() {
        for (concept in CreatureConcept.entries) {
            assertThat(VoiceCharacter.of(concept, -3f, 9f))
                .isEqualTo(VoiceCharacter.of(concept, 0f, 1f))
        }
    }
}
