package app.anima.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SoulPortTest {
    private val stats =
        RelationshipStats(
            hatchedAtMillis = 0L,
            conversationCount = 12,
            liveFactCount = 3,
            chargeCount = 40,
        )

    @Test
    fun `export is readable markdown with counters and categories`() {
        val md =
            SoulPort.export(
                creatureName = "Люмик",
                concept = CreatureConcept.SPIRIT_ORB,
                stats = stats,
                facts =
                    listOf(
                        SoulFact("f-1", FactCategory.IDENTITY, "my person is Nick", FactSource.ONBOARDING, 0L),
                        SoulFact("f-2", FactCategory.PREFERENCE, "loves rain", FactSource.CHAT_CONFIRMED, 0L),
                        SoulFact(
                            "f-3",
                            FactCategory.PREFERENCE,
                            "hates mornings",
                            FactSource.CHAT_CONFIRMED,
                            0L,
                            forgottenAtMillis = 5L,
                        ),
                    ),
                journal = listOf(BodyJournalEntry("j-1", JournalKind.HATCHED, 0L, "first light")),
                nowMillis = RelationshipStats.DAY_MILLIS * 9,
            )
        assertThat(md).contains(SoulPort.FORMAT_MARKER)
        assertThat(md).contains("# The soul of Люмик")
        assertThat(md).contains("Days together: 10")
        assertThat(md).contains("### Identity")
        assertThat(md).contains("loves rain")
        assertThat(md).doesNotContain("hates mornings") // forgotten stays private
    }

    @Test
    fun `export import roundtrip preserves live facts as candidates`() {
        val facts =
            listOf(
                SoulFact("f-1", FactCategory.IDENTITY, "my person is Nick", FactSource.ONBOARDING, 0L),
                SoulFact("f-2", FactCategory.PREFERENCE, "loves rain", FactSource.CHAT_CONFIRMED, 0L),
                SoulFact("f-3", FactCategory.WORK, "ships Android apps", FactSource.CHAT_CONFIRMED, 0L),
            )
        val md = SoulPort.export("A", CreatureConcept.MOTH, stats, facts, emptyList(), 1L)
        val candidates = SoulPort.parseCandidates(md)
        assertThat(candidates.map { it.text }).containsAtLeast(
            "my person is Nick",
            "loves rain",
            "ships Android apps",
        )
        val byText = candidates.associateBy { it.text }
        assertThat(byText["my person is Nick"]!!.category).isEqualTo(FactCategory.IDENTITY)
        assertThat(byText["ships Android apps"]!!.category).isEqualTo(FactCategory.WORK)
    }

    @Test
    fun `parse tolerates alien AI output`() {
        val alien =
            """
            Sure! Here's what I know about you:
            ## Preferences
            - You drink your coffee black
            * You like winter
            Some prose in between that is not a bullet.
            ## Work
            - [work] building an app called Anima
            - ${"y".repeat(400)}
            """.trimIndent()
        val candidates = SoulPort.parseCandidates(alien)
        assertThat(candidates.map { it.text }).containsExactly(
            "You drink your coffee black",
            "You like winter",
            "building an app called Anima",
        )
        assertThat(candidates[0].category).isEqualTo(FactCategory.PREFERENCE)
        assertThat(candidates[2].category).isEqualTo(FactCategory.WORK)
    }

    @Test
    fun `parse dedups case-insensitively and caps volume`() {
        val spam =
            buildString {
                appendLine("- Same fact")
                appendLine("- same FACT")
                repeat(500) { appendLine("- unique fact $it") }
            }
        val candidates = SoulPort.parseCandidates(spam)
        assertThat(candidates.count { it.text.lowercase() == "same fact" }).isEqualTo(1)
        assertThat(candidates.size).isAtMost(SoulPort.MAX_IMPORT_FACTS)
    }

    @Test
    fun `days together starts at one`() {
        assertThat(stats.daysTogether(nowMillis = 60_000L)).isEqualTo(1)
    }

    /**
     * v0.6: the extractor prompt is localized and pins English headings,
     * but assistants localize them anyway — the parser must speak all six
     * product languages (one representative per locale here).
     */
    @Test
    fun `parse recognizes localized headings from all six locales`() {
        val md =
            """
            ## Identität
            - mag stille Morgen
            ## Preferencias
            - le encanta la lluvia
            ## Ludzie
            - siostra ma na imię Ola
            ## 仕事
            - Androidアプリを作っている
            ## Моменты
            - первый снег вместе
            """.trimIndent()
        val byCat = SoulPort.parseCandidates(md).associateBy { it.category }
        assertThat(byCat[FactCategory.IDENTITY]?.text).isEqualTo("mag stille Morgen")
        assertThat(byCat[FactCategory.PREFERENCE]?.text).isEqualTo("le encanta la lluvia")
        assertThat(byCat[FactCategory.PEOPLE]?.text).isEqualTo("siostra ma na imię Ola")
        assertThat(byCat[FactCategory.WORK]?.text).isEqualTo("Androidアプリを作っている")
        assertThat(byCat[FactCategory.MOMENT]?.text).isEqualTo("первый снег вместе")
    }
}
