package app.anima.core.model

/**
 * v1.1c — where a creature's body came from, and therefore who is allowed to
 * change it.
 *
 * The identity repair in `IdentityRepair` recomputes a body from its seed and
 * writes the answer down. That is right for a body this build assigned and
 * catastrophic for one it did not: builds up to and including v1.1b showed a
 * gallery of all eight concepts at hatch and let a tap in Settings replace the
 * body afterwards. For those souls a concept that differs from `assignedTo` is
 * not corruption — it is the owner's own decision, and rewriting it would
 * delete a lived identity in the name of protecting it.
 *
 * So the era is recorded, and only [ASSIGNED] is subject to repair.
 *
 * Stored as a meta row; **absence means [CHOSEN]**, because every soul that
 * predates this enum was hatched by a build that asked.
 */
enum class IdentityOrigin(
    val wire: String,
) {
    /**
     * Hatched by a build that derives the body from the device seed. The
     * invariant `concept == CreatureConcept.assignedTo(seed)` holds by
     * construction, so a violation is real damage and can be repaired.
     */
    ASSIGNED("assigned"),

    /**
     * Restored from a soul file. The body is authoritative — it belonged to the
     * creature on its previous phone — and is not a function of this device's
     * seed. Never repaired.
     */
    TRANSFERRED("transferred"),

    /**
     * Hatched by a build that offered a choice (up to and including v1.1b), or
     * switched by a tap in the old Settings body grid. The body is the owner's
     * decision. Never repaired.
     */
    CHOSEN("chosen"),
    ;

    companion object {
        /** Unknown or absent → [CHOSEN]: the conservative answer is "do not touch it". */
        fun fromWire(wire: String?): IdentityOrigin = entries.firstOrNull { it.wire == wire } ?: CHOSEN
    }
}
