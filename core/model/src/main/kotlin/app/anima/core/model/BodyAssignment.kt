package app.anima.core.model

/**
 * v1.1c — the function this product said it had and did not.
 *
 * Four documents in `docs/` state that the body is assigned to the phone and
 * never chosen ("The body is assigned, never chosen", handoff invariant 7). The
 * code did the opposite: onboarding showed a gallery of all eight concepts and
 * Settings let one tap replace the body. The seed shaped the *genome* — hue,
 * scale, blink rate — but the concept came from a finger. Recorded with the
 * grep in `docs/design/v11/phase0-v11c-audit.md` §4.
 *
 * ## Why this is not `entries[seed % entries.size]`
 *
 * An index mapping ties every living creature to the *size* of the set. Add a
 * ninth body and eight-ninths of all creatures become someone else overnight;
 * remove one and the whole table shifts. For a product whose single promise is
 * "this body belongs to this phone", that is the worst available failure.
 *
 * So the assignment is a **tournament, not an index**: each concept computes a
 * score from (seed, its own wire name), and the highest score wins. This is
 * rendezvous hashing, and it has the property the set needs —
 *
 *  * removing a concept moves **only** the creatures that concept had won.
 *    Everyone else keeps their body, because their winner's score did not
 *    change and neither did their runners-up;
 *  * adding a concept takes a proportional share from everyone and moves
 *    nobody else;
 *  * reordering the enum changes nothing at all, because the score depends on
 *    the wire string, not on the declaration order.
 *
 * Held by `BodyAssignmentTest`, which checks all three properties over 100 000
 * seeds rather than asserting them in prose.
 *
 * ## What it does not do
 *
 * It does not re-decide the body of a creature that already has one. Assignment
 * happens once, at hatch, and the answer is written to durable storage; from
 * then on the stored value is the truth. A soul that was hatched by a build
 * which offered a choice keeps the body its owner chose — see
 * `IdentityOrigin` and `docs/adr/ADR-025-assigned-body.md`.
 */
fun CreatureConcept.Companion.assignedTo(seed: Long): CreatureConcept =
    CreatureConcept.entries.maxBy { concept -> assignmentScore(seed, concept.wire) }

/**
 * The per-(seed, body) score. FNV-1a over the wire name folded into the seed,
 * then a SplitMix64 finalizer so neighbouring seeds do not produce neighbouring
 * scores. Pure, allocation-free, and identical on every platform because it is
 * all `Long` arithmetic.
 */
private fun assignmentScore(
    seed: Long,
    wire: String,
): Long {
    var hash = FNV_OFFSET_BASIS
    for (ch in wire) {
        hash = hash xor ch.code.toLong()
        hash *= FNV_PRIME
    }
    var z = seed xor hash
    z += GOLDEN_GAMMA
    z = (z xor (z ushr 30)) * MIX_A
    z = (z xor (z ushr 27)) * MIX_B
    z = z xor (z ushr 31)
    // Unsigned compare: a signed max would give every negative-scoring body a
    // structural disadvantage, and roughly half of all scores are negative.
    return z ushr 1
}

private const val FNV_OFFSET_BASIS = -0x340d631b7bdddcdbL
private const val FNV_PRIME = 0x100000001b3L
private const val GOLDEN_GAMMA = -0x61c8864680b583ebL
private const val MIX_A = -0x40a7b892e31b1a47L
private const val MIX_B = -0x6b2fb644ecceee15L
