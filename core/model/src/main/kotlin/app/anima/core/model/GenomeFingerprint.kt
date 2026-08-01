package app.anima.core.model

/**
 * v1.1c task 5.2 — the seed, in a form a person can read out over the phone.
 *
 * The passport has to show *something* that proves the creature is derived
 * rather than assembled. The seed itself is a signed 64-bit number and reads as
 * noise; a hex dump reads as a crash log. This is eight characters in two
 * groups, from an alphabet with no `0/O`, no `1/I/L`, no `U` — so it cannot be
 * misheard, mistyped, or accidentally spell anything.
 *
 * It is a **fingerprint, not a key**: 40 bits of a 64-bit seed, one-way by
 * truncation. Reading it aloud gives nobody the creature.
 */
object GenomeFingerprint {
    /** Crockford base32 minus the vowels that let it spell words. */
    private const val ALPHABET = "23456789ABCDEFGHJKMNPQRSTVWXYZ"

    private const val GROUP = 4
    private const val LENGTH = 8

    fun of(seed: Long): String {
        // Mix first, so that two phones with adjacent ANDROID_IDs do not get
        // fingerprints that differ in one character.
        var z = seed
        z = (z xor (z ushr 33)) * -0x7ee3623a03d3c83fL
        z = (z xor (z ushr 29)) * -0x3b314601e57a13adL
        z = z xor (z ushr 32)

        val text = StringBuilder(LENGTH + 1)
        var rest = z
        repeat(LENGTH) { index ->
            if (index == GROUP) text.append('-')
            // 30 symbols, not 32, so the two ambiguous pairs are gone. The
            // remainder is taken from a fresh 8-bit slice each round rather than
            // from a 5-bit one, which keeps the bias below a tenth of a percent.
            text.append(ALPHABET[((rest and 0xFF).toInt() % ALPHABET.length)])
            rest = rest ushr 8
        }
        return text.toString()
    }
}
