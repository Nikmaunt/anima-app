package app.anima.core.model

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

class CreatureNameTest {
    private val seeds: List<Long> =
        (0 until SWEEP).map { i ->
            var z = i.toLong() * -0x61c8864680b583ebL
            z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
            z xor (z ushr 31)
        }

    @Test
    fun `the same seed is always the same name`() {
        seeds.take(2000).forEach { assertThat(CreatureName.forSeed(it)).isEqualTo(CreatureName.forSeed(it)) }
    }

    @Test
    fun `every name fits the header it goes in`() {
        seeds.forEach { seed ->
            val name = CreatureName.forSeed(seed)
            assertWithMessage("'$name' from seed $seed")
                .that(name.length)
                .isIn(MIN_LENGTH..MAX_LENGTH)
        }
    }

    @Test
    fun `every name is capitalised Latin letters and nothing else`() {
        seeds.forEach { seed ->
            val name = CreatureName.forSeed(seed)
            assertWithMessage("'$name' from seed $seed").that(name).matches("[A-Z][a-z]+")
        }
    }

    @Test
    fun `no name reads as something an owner would not want`() {
        val blocked =
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
        seeds.forEach { seed ->
            val lower = CreatureName.forSeed(seed).lowercase()
            val hit = blocked.firstOrNull { it in lower }
            assertWithMessage("seed $seed produced '$lower', which contains '$hit'").that(hit).isNull()
        }
    }

    /**
     * Not uniqueness — a name is not an id, and two phones may share one. But a
     * generator that collapses onto a handful of names would make the creature
     * feel mass-produced, which is the whole thing this product is not.
     */
    @Test
    fun `the names are varied`() {
        val distinct = seeds.map { CreatureName.forSeed(it) }.toSet()
        assertWithMessage("only ${distinct.size} distinct names in $SWEEP seeds")
            .that(distinct.size)
            .isGreaterThan(MIN_DISTINCT)
    }

    /**
     * Pinned, for the same reason the body assignment is pinned: changing the
     * generator renames every creature that has not hatched yet.
     */
    @Test
    fun `the generator is pinned`() {
        assertThat(listOf(0L, 1L, -1L, 909_090L).map(CreatureName::forSeed))
            .containsExactly("Tife", "Kazar", "Vele", "Sokane")
            .inOrder()
    }

    private companion object {
        const val SWEEP = 50_000
        const val MIN_LENGTH = 4
        const val MAX_LENGTH = 7
        const val MIN_DISTINCT = 5_000
    }
}
