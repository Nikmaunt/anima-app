package app.anima.core.model

/**
 * The owner-tunable character (v0.2 Phase 2). Two axes, both 0..1:
 *
 * - [warmth]: 0 = soft and gentle … 1 = dry and snarky (never cruel — the
 *   persona keeps the affection constant, only the surface changes).
 * - [chattiness]: 0 = few words … 1 = talkative.
 *
 * The axes reach BOTH the mind (PromptBuilder persona) and the body
 * ([tunedGenome]: saccade frequency via curiosity, motion amplitude via
 * blink/size nudges) so a snarky creature also *moves* snappier.
 */
data class Personality(
    val warmth: Float,
    val chattiness: Float,
) {
    fun clamped(): Personality = Personality(warmth.coerceIn(0f, 1f), chattiness.coerceIn(0f, 1f))

    companion object {
        val Default = Personality(warmth = 0.25f, chattiness = 0.5f)

        /** Each of the 8 concepts leans a certain way out of the egg. */
        fun presetFor(concept: CreatureConcept): Personality =
            when (concept) {
                CreatureConcept.SPIRIT_ORB -> Personality(0.15f, 0.35f)
                CreatureConcept.SPROUT -> Personality(0.05f, 0.45f)
                CreatureConcept.FOX_KIT -> Personality(0.65f, 0.75f)
                CreatureConcept.JELLY -> Personality(0.2f, 0.6f)
                CreatureConcept.MOTH -> Personality(0.3f, 0.2f)
                CreatureConcept.PIXEL_PET -> Personality(0.55f, 0.65f)
                CreatureConcept.ROBOT -> Personality(0.45f, 0.3f)
                CreatureConcept.EMBER -> Personality(0.75f, 0.5f)
            }
    }
}

/**
 * Rig-side application: personality nudges the genome within its documented
 * bounds, so no combination can produce an unreadable creature.
 */
fun CreatureGenome.tunedBy(personality: Personality): CreatureGenome {
    val p = personality.clamped()
    return copy(
        // Chatty creatures dart and follow more; quiet ones gaze longer.
        curiosity = (curiosity * 0.5f + p.chattiness * 0.5f).coerceIn(0f, 1f),
        // Snark reads as quicker blinks; softness as slow, calm lids.
        blinkRateScale = (blinkRateScale * (1.15f - p.warmth * 0.3f)).coerceIn(0.8f, 1.3f),
    )
}

/** Prompt-side application, shared by every mind backend (ADR-005). */
object PersonaTuning {
    fun toneLines(personality: Personality): String {
        val p = personality.clamped()
        val tone =
            when {
                p.warmth < 0.33f -> "Your tone is soft, warm and encouraging."
                p.warmth < 0.66f -> "Your tone is friendly with an occasional dry aside."
                else ->
                    "Your tone is dry and playfully snarky — teasing, never mean; " +
                        "your affection shows through the snark."
            }
        val length =
            when {
                p.chattiness < 0.33f -> "You answer in one short sentence unless asked for more."
                p.chattiness < 0.66f -> "You answer briefly, one to three sentences."
                else -> "You are talkative: two to four lively sentences, with little tangents."
            }
        return "$tone $length"
    }
}
