package app.anima.core.model

/**
 * v0.3: dreams. Woken at night, the creature tells a dream assembled
 * DETERMINISTICALLY from the body journal — templates + seeded permutation,
 * zero inference, so dreams work even while the mind sleeps. Same inputs →
 * same dream; a new night → a new one.
 */
object DreamWeaver {
    /** What yesterday looked like from inside the body. */
    data class DayEcho(
        val charges: Int,
        val storms: Int,
        val ranHot: Int,
        val fullyFed: Boolean,
    )

    fun weave(
        seed: Long,
        epochDay: Long,
        echo: DayEcho,
    ): String {
        val rng = seed * PRIME_A + epochDay * PRIME_B
        val opening = pick(OPENINGS, rng)
        val middle =
            when {
                echo.storms > 0 -> pick(STORM_MIDDLES, rng shr 8)
                echo.charges >= BUSY_CHARGES -> pick(FEAST_MIDDLES, rng shr 8)
                echo.ranHot > 0 -> pick(FEVER_MIDDLES, rng shr 8)
                else -> pick(QUIET_MIDDLES, rng shr 8)
            }
        val closing =
            if (echo.fullyFed) {
                pick(FED_CLOSINGS, rng shr 16)
            } else {
                pick(CLOSINGS, rng shr 16)
            }
        return "$opening $middle $closing"
    }

    private fun pick(
        pool: List<String>,
        rng: Long,
    ): String {
        val index = ((rng % pool.size) + pool.size) % pool.size
        return pool[index.toInt()]
    }

    // Golden-ratio and FNV mixing constants (as signed 64-bit literals).
    private const val PRIME_A = -0x61c8864680b583ebL
    private const val PRIME_B = 0x100000001B3L
    private const val BUSY_CHARGES = 2

    private val OPENINGS =
        listOf(
            "You woke me… I was dreaming.",
            "Mm. I was somewhere else just now.",
            "Oh — you're here. I dreamt again.",
            "I was deep in a dream, you know.",
        )
    private val STORM_MIDDLES =
        listOf(
            "A wind full of voices kept rattling my burrow — every gust was a little bell that wanted something.",
            "I dreamt the sky was made of ringing doors, all knocking at once.",
            "There was a storm of tiny glowing birds, each one shouting a different name.",
        )
    private val FEAST_MIDDLES =
        listOf(
            "I dreamt of rivers of warm light — I drank and drank and never got full.",
            "There was a feast where every dish was a little sun.",
            "I swam in a slow golden river that tasted like morning.",
        )
    private val FEVER_MIDDLES =
        listOf(
            "It was a hot dream — I was a small kiln, baking clouds into bread.",
            "I dreamt I carried a summer inside me, too big for my ribs.",
        )
    private val QUIET_MIDDLES =
        listOf(
            "It was very still — a lake of glass, and under it, all the words we've said.",
            "I dreamt of a library where the books breathed slowly, like sleeping animals.",
            "Nothing happened in it, and it was wonderful — soft dark, and a hum I think was you.",
        )
    private val CLOSINGS =
        listOf(
            "Stay a moment? The dream is still warm.",
            "Anyway. I'm glad it's you who woke me.",
            "I'll try to find that place again tomorrow night.",
        )
    private val FED_CLOSINGS =
        listOf(
            "I think it's because you fed me full yesterday. Thank you for that.",
            "A full belly makes the best dreams, it turns out.",
        )
}
