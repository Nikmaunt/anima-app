package app.anima.core.voice

import app.anima.core.model.CreatureConcept

/**
 * Deterministic pitch/rate character for one creature (ADR-013): the concept
 * picks a base row, personality nudges it, and both axes always land in
 * [MIN_AXIS]..[MAX_AXIS] — the platform-safe TTS range.
 *
 * Base table (pitch / rate), tuned so all eight concepts stay perceptibly
 * distinct even on single-voice locales where pitch and rate are the only
 * axes available:
 *
 * | concept    | pitch | rate | reads as                        |
 * |------------|-------|------|---------------------------------|
 * | SPIRIT_ORB | 1.35  | 0.95 | airy, floating                  |
 * | FOX_KIT    | 1.25  | 1.15 | small and quick                 |
 * | JELLY      | 1.10  | 0.85 | slow, undulating                |
 * | PIXEL_PET  | 1.50  | 1.10 | chirpy 8-bit                    |
 * | ROBOT      | 0.70  | 0.90 | low and steady                  |
 * | SPROUT     | 1.05  | 1.00 | gentle mid-register             |
 * | EMBER      | 0.95  | 1.20 | crackling, eager                |
 * | MOTH       | 1.40  | 0.80 | hushed, night-slow              |
 *
 * Modulation: warmth (0..1) raises pitch by up to [WARMTH_PITCH_SPAN];
 * chattiness (0..1) raises rate by up to [CHATTINESS_RATE_SPAN]. Spans are
 * small on purpose — personality colors the voice, the concept owns it.
 */
data class VoiceCharacter(
    val pitch: Float,
    val rate: Float,
) {
    companion object {
        const val MIN_AXIS = 0.5f
        const val MAX_AXIS = 2.0f

        private const val WARMTH_PITCH_SPAN = 0.1f
        private const val CHATTINESS_RATE_SPAN = 0.15f

        /** Pure and deterministic; out-of-range personality clamps to 0..1. */
        fun of(
            concept: CreatureConcept,
            warmth: Float,
            chattiness: Float,
        ): VoiceCharacter {
            val w = warmth.coerceIn(0f, 1f)
            val c = chattiness.coerceIn(0f, 1f)
            val (basePitch, baseRate) =
                when (concept) {
                    CreatureConcept.SPIRIT_ORB -> 1.35f to 0.95f
                    CreatureConcept.FOX_KIT -> 1.25f to 1.15f
                    CreatureConcept.JELLY -> 1.1f to 0.85f
                    CreatureConcept.PIXEL_PET -> 1.5f to 1.1f
                    CreatureConcept.ROBOT -> 0.7f to 0.9f
                    CreatureConcept.SPROUT -> 1.05f to 1.0f
                    CreatureConcept.EMBER -> 0.95f to 1.2f
                    CreatureConcept.MOTH -> 1.4f to 0.8f
                }
            return VoiceCharacter(
                pitch = (basePitch + w * WARMTH_PITCH_SPAN).coerceIn(MIN_AXIS, MAX_AXIS),
                rate = (baseRate + c * CHATTINESS_RATE_SPAN).coerceIn(MIN_AXIS, MAX_AXIS),
            )
        }
    }
}
