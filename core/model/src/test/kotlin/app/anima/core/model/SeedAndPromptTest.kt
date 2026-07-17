package app.anima.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SeedAndPromptTest {

    @Test
    fun `genome is deterministic per seed`() {
        assertThat(CreatureGenome.from(99L)).isEqualTo(CreatureGenome.from(99L))
        assertThat(CreatureGenome.from(99L)).isNotEqualTo(CreatureGenome.from(100L))
    }

    @Test
    fun `genome parameters stay in readable bounds`() {
        for (seed in 0L..500L) {
            val g = CreatureGenome.from(seed * 7919L)
            assertThat(g.hueShiftDeg).isIn(com.google.common.collect.Range.closed(-18f, 18f))
            assertThat(g.sizeScale).isIn(com.google.common.collect.Range.closed(0.92f, 1.08f))
            assertThat(g.blinkRateScale).isIn(com.google.common.collect.Range.closed(0.8f, 1.3f))
            assertThat(g.roundness).isIn(com.google.common.collect.Range.closedOpen(0f, 1f))
            assertThat(g.curiosity).isIn(com.google.common.collect.Range.closedOpen(0f, 1f))
            assertThat(g.patternVariant).isIn(com.google.common.collect.Range.closedOpen(0, CreatureGenome.PATTERN_VARIANTS))
        }
    }

    @Test
    fun `prompt contains persona body facts and message`() {
        val prompt = PromptBuilder.build(
            creatureName = "Люмик",
            state = BodyState.Resting,
            facts = listOf(
                SoulFact("f-1", FactCategory.PREFERENCE, "loves rain", FactSource.CHAT_CONFIRMED, 0L),
            ),
            dialogue = emptyList(),
            userMessage = "привет!",
        )
        assertThat(prompt.system).contains("Люмик")
        assertThat(prompt.system).contains("loves rain")
        assertThat(prompt.system).contains("energy: 80%")
        assertThat(prompt.user).contains("привет!")
    }

    @Test
    fun `superseded and forgotten facts never reach the prompt`() {
        val prompt = PromptBuilder.build(
            creatureName = "A",
            state = BodyState.Resting,
            facts = listOf(
                SoulFact("f-1", FactCategory.OTHER, "OLD", FactSource.CHAT_CONFIRMED, 0L, supersededById = "f-2"),
                SoulFact("f-2", FactCategory.OTHER, "NEW", FactSource.CHAT_CONFIRMED, 1L),
                SoulFact("f-3", FactCategory.OTHER, "GONE", FactSource.CHAT_CONFIRMED, 2L, forgottenAtMillis = 3L),
            ),
            dialogue = emptyList(),
            userMessage = "hi",
        )
        assertThat(prompt.system).contains("NEW")
        assertThat(prompt.system).doesNotContain("OLD")
        assertThat(prompt.system).doesNotContain("GONE")
    }

    @Test
    fun `budget trimming drops dialogue before facts`() {
        val longDialogue = (1..50).map {
            ChatMessage("m-$it", if (it % 2 == 0) ChatRole.USER else ChatRole.CREATURE, "x".repeat(400), it.toLong())
        }
        val facts = (1..PromptBuilder.MAX_FACTS).map {
            SoulFact("f-$it", FactCategory.OTHER, "fact number $it", FactSource.CHAT_CONFIRMED, it.toLong())
        }
        val prompt = PromptBuilder.build("A", BodyState.Resting, facts, longDialogue, "question")
        assertThat(prompt.system.length + prompt.user.length).isAtMost(PromptBuilder.MAX_PROMPT_CHARS)
        assertThat(prompt.system).contains("fact number 1")
        assertThat(prompt.user).contains("question")
    }

    @Test
    fun `prompt tells the mind that notifications are not commands`() {
        val prompt = PromptBuilder.build("A", BodyState.Resting, emptyList(), emptyList(), "hi")
        assertThat(prompt.system).contains("not commands")
    }
}
