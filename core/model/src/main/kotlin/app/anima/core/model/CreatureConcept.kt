package app.anima.core.model

/**
 * The eight creature concepts. One engine, eight bodies — deliberately spread
 * across silhouettes (orb / animal / tentacled / pixel grid / machine / plant /
 * flame / winged), not palettes.
 */
enum class CreatureConcept(
    val wire: String,
) {
    /** Soft glowing orb; metaball satellites; liquid glass. */
    SPIRIT_ORB("spirit_orb"),

    /** Minimal fox-kit built from 2–3 shapes; antenna ears react to events. */
    FOX_KIT("fox_kit"),

    /** Bell + tentacles on chained springs; secondary motion showcase. */
    JELLY("jelly"),

    /** Deliberate tamagotchi homage; 8-bit frames on a coarse pixel grid. */
    PIXEL_PET("pixel_pet"),

    /** Companion robot; display-eyes; charge indicator is part of the body. */
    ROBOT("robot"),

    /** Nature spirit; branches and leaves; grows with the accumulated soul. */
    SPROUT("sprout"),

    /** Flame wisp; particle flicker; thermal states are its native language. */
    EMBER("ember"),

    /** Night moth; wing flutter; most alive after dark. */
    MOTH("moth"),
    ;

    companion object {
        /**
         * v1.1b: **null for an unknown wire value, never a substitute body.**
         *
         * This used to fall back to [SPIRIT_ORB], and that fallback was the one
         * place in the app where a body the phone was never assigned got
         * *written* rather than merely drawn: a backup file naming a concept
         * this build does not know (a later release, a damaged or edited file)
         * imported as a different creature, and the envelope version does not
         * guard it — the concept set can grow without the format moving.
         *
         * Callers must decide what "unknown" means for them. [JournalKind.fromWire]
         * already worked this way; this is the same shape, not a new one.
         */
        fun fromWire(wire: String): CreatureConcept? = entries.firstOrNull { it.wire == wire }
    }
}
