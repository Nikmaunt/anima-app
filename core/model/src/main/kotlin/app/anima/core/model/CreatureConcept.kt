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
        fun fromWire(wire: String): CreatureConcept = entries.firstOrNull { it.wire == wire } ?: SPIRIT_ORB
    }
}
