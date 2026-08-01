package app.anima.core.model

/**
 * v1.1c task 5.4 — the name arrives with the body, from the same seed.
 *
 * Onboarding used to ask for a name in a text field before the creature had
 * done anything. That is a form, and a form is the wrong first thing: it asks
 * the owner to invent a relationship rather than meet one. The creature now
 * arrives already called something, and renaming stays available forever in
 * Settings — which is how naming works with anything that turns up alive.
 *
 * ## Constraints this had to satisfy
 *
 * * **Deterministic.** Same seed, same name, on every launch and after any
 *   reinstall onto the same phone. It is part of the identity, not a roll.
 * * **Pronounceable in every locale the app ships.** The app runs in six
 *   languages; a name is read aloud in the owner's head. Consonant-vowel
 *   syllables from a deliberately small alphabet — no `x`, no consonant
 *   clusters, no letters outside the Latin core — so the result is sayable in
 *   ru/pl/ja/es/de/en alike and transliterates cleanly.
 * * **Not a word.** Generated from syllables rather than picked from a list of
 *   real names, so it belongs to nobody and means nothing in any language. A
 *   short blocklist keeps the arithmetic from producing something that reads
 *   badly anyway.
 * * **Short.** Two or three syllables, 4–7 letters. It goes in a 32sp Light
 *   header on Home and in a widget label.
 */
object CreatureName {
    /**
     * The name this seed's creature is born with. Pure; no locale, no clock,
     * no storage.
     */
    fun forSeed(seed: Long): String {
        var attempt = 0
        while (attempt < MAX_ATTEMPTS) {
            val candidate = compose(seed, attempt)
            val lower = candidate.lowercase()
            if (BLOCKED.none { it in lower }) return candidate
            attempt++
        }
        // Unreachable in practice — the blocklist covers a vanishing fraction of
        // the space and each attempt is an independent draw. Bounded anyway,
        // because an unbounded loop in identity code is how a launch hangs.
        return compose(seed, MAX_ATTEMPTS)
    }

    private fun compose(
        seed: Long,
        attempt: Int,
    ): String {
        val rng = SplitMix64(seed xor (attempt.toLong() * SALT))
        // Three syllables about a third of the time; two otherwise. Enough
        // variety that two phones side by side rarely rhyme, few enough that
        // the name stays a name and not a password.
        val syllables = if (rng.nextInt(SYLLABLE_ODDS) == 0) 3 else 2
        val text = StringBuilder()
        repeat(syllables) { index ->
            text.append(ONSETS[rng.nextInt(ONSETS.size)])
            text.append(VOWELS[rng.nextInt(VOWELS.size)])
            // One soft coda at most, and only on the last syllable: it is what
            // makes "Lumen" rather than "Lume", and confining it to the end
            // keeps the name inside 4–7 letters for the 32sp header on Home.
            if (index == syllables - 1 && rng.nextInt(CODA_ODDS) == 0) {
                text.append(CODAS[rng.nextInt(CODAS.size)])
            }
        }
        return text[0].uppercaseChar() + text.substring(1)
    }

    /** Consonants that exist with the same sound in all six shipped locales. */
    private val ONSETS = listOf("m", "n", "l", "r", "s", "t", "k", "v", "f", "p", "b", "d", "z", "h")

    private val VOWELS = listOf("a", "e", "i", "o", "u")

    /** Codas that never create a cluster a Japanese or Spanish reader stumbles on. */
    private val CODAS = listOf("n", "m", "l", "r", "s")

    /**
     * Not a moral filter — a legibility one. Substrings the syllable arithmetic
     * can reach that read as a real word, a slur, or an obscenity in one of the
     * six shipped locales. A creature is named once and lives with it; the cost
     * of a short list here is much lower than the cost of one bad draw.
     */
    private val BLOCKED =
        listOf(
            "mama",
            "papa",
            "baba",
            "dada",
            "kaka",
            "pipi",
            "popo",
            "sisi",
            "kuka",
            "nazi",
            "nasi",
            "puta",
            "puto",
            "suka",
            "durak",
            "loh",
            "muda",
            "hui",
            "hue",
            "pisa",
            "sos",
            "lol",
            "kur",
            "bler",
            "zalu",
        )

    private const val SALT = 0x51ED270B279E7DFL
    private const val SYLLABLE_ODDS = 3
    private const val CODA_ODDS = 3
    private const val MAX_ATTEMPTS = 16
}
