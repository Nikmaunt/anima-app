package app.anima.core.mind

import app.anima.core.model.FactCategory
import app.anima.core.model.FactJson
import app.anima.core.model.MindEvent
import app.anima.core.model.MindPrompt
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MindTest {
    @Test
    fun `fake streams word chunks then done with full text`() =
        runTest {
            val fake = FakeMindEngine(handler = { _, _ -> "hello little phone" })
            val events = fake.reply(MindPrompt("s", "u")).toList()
            val chunks = events.filterIsInstance<MindEvent.Chunk>()
            val done = events.last() as MindEvent.Done
            assertThat(chunks.joinToString("") { it.text }).isEqualTo("hello little phone")
            assertThat(done.fullText).isEqualTo("hello little phone")
        }

    @Test
    fun `fake handler sees call index for retry scripting`() =
        runTest {
            val fake = FakeMindEngine(handler = { _, index -> "call-$index" })
            val first = fake.reply(MindPrompt("s", "u")).toList().last() as MindEvent.Done
            val second = fake.reply(MindPrompt("s", "u")).toList().last() as MindEvent.Done
            assertThat(first.fullText).isEqualTo("call-0")
            assertThat(second.fullText).isEqualTo("call-1")
        }

    @Test
    fun `fact json parses clean output`() {
        val parsed =
            FactJson.parseCandidates(
                """[{"category":"preference","text":"loves rain"},{"category":"work","text":"ships apps"}]""",
            )
        assertThat(parsed).hasSize(2)
        assertThat(parsed[0].category).isEqualTo(FactCategory.PREFERENCE)
        assertThat(parsed[0].text).isEqualTo("loves rain")
    }

    @Test
    fun `fact json survives prose-wrapped and dirty output`() {
        val parsed =
            FactJson.parseCandidates(
                "Sure! Here you go:\n[{\"category\":\"unknown_future\",\"text\":\"a fact\"}," +
                    " {\"text\":\"\"}]\nHope this helps!",
            )
        assertThat(parsed).hasSize(1)
        assertThat(parsed[0].category).isEqualTo(FactCategory.OTHER)
    }

    @Test
    fun `fact json returns empty on garbage`() {
        assertThat(FactJson.parseCandidates("no json here")).isEmpty()
        assertThat(FactJson.parseCandidates("[not json]")).isEmpty()
        assertThat(FactJson.parseCandidates("")).isEmpty()
    }

    @Test
    fun `fact json caps volume`() {
        val many = (1..20).joinToString(",", "[", "]") { """{"category":"other","text":"f$it"}""" }
        assertThat(FactJson.parseCandidates(many)).hasSize(FactJson.MAX_CANDIDATES)
    }

    // ADR-011 data-scope guard (audit-v03 F1): the local-only view must never
    // pick CLOUD, whatever the cloud's state; the full ladder still does.

    @Test
    fun `tier selection never picks cloud when the surface excludes it`() {
        assertThat(
            TierSelection.pick(cloudReady = false, nanoAwake = false, hasLocalModel = false),
        ).isEqualTo(app.anima.core.model.MindTier.NONE)
        // The digest path passes cloudReady=false unconditionally; even a
        // ready cloud plus no local tier must fall through to NONE.
        assertThat(
            TierSelection.pick(cloudReady = false, nanoAwake = false, hasLocalModel = true),
        ).isEqualTo(app.anima.core.model.MindTier.GEMMA)
        assertThat(
            TierSelection.pick(cloudReady = false, nanoAwake = true, hasLocalModel = true),
        ).isEqualTo(app.anima.core.model.MindTier.NANO)
    }

    @Test
    fun `tier selection prefers cloud only when allowed and ready`() {
        assertThat(
            TierSelection.pick(cloudReady = true, nanoAwake = true, hasLocalModel = true),
        ).isEqualTo(app.anima.core.model.MindTier.CLOUD)
    }
}
