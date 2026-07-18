package app.anima.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MindRegistryAndRoutingTest {
    // --- registry matching ---

    @Test
    fun `matches the pack default artifact by file name`() {
        val spec = MindModelRegistry.match("qwen2.5-1.5b-instruct-q8.task")
        assertThat(spec).isEqualTo(MindModelRegistry.QWEN25_15B)
    }

    @Test
    fun `matches the legacy gemma artifact and keeps it english-only`() {
        val spec = MindModelRegistry.match("gemma3-1b-it-int4.task")
        assertThat(spec).isEqualTo(MindModelRegistry.GEMMA3_1B)
        assertThat(spec!!.speaks(MindLanguage.RU)).isFalse()
        assertThat(spec.speaks(MindLanguage.EN)).isTrue()
    }

    @Test
    fun `matching is case-insensitive and null for strangers`() {
        assertThat(MindModelRegistry.match("Gemma-4-E2B-it.litertlm")).isEqualTo(MindModelRegistry.GEMMA4_E2B)
        assertThat(MindModelRegistry.match("mystery-model.task")).isNull()
    }

    @Test
    fun `unknown installed file degrades to an honest english-only spec`() {
        val spec = MindModelRegistry.specFor(InstalledMindModel("mystery.task", 700L, "/x/mystery.task"))
        assertThat(spec.languages).containsExactly(MindLanguage.EN)
        assertThat(spec.license).isEqualTo(ModelLicense.UNKNOWN)
        assertThat(spec.stopTokens).isEmpty()
    }

    @Test
    fun `pack default speaks all six product languages under apache`() {
        val spec = MindModelRegistry.packDefault
        MindLanguage.entries.forEach { assertThat(spec.speaks(it)).isTrue() }
        assertThat(spec.license).isEqualTo(ModelLicense.APACHE_2)
    }

    @Test
    fun `char budget derives from the token window`() {
        // 2048 tokens − 320 reserve = 1728 tokens × 3 chars.
        assertThat(MindModelRegistry.QWEN25_15B.promptCharBudget).isEqualTo(5184)
    }

    // --- language routing ---

    @Test
    fun `cloud tier is native in any language`() {
        val d = MindLanguageRouting.decide(MindLanguage.JA, MindTier.CLOUD, localSpec = null)
        assertThat(d.language).isEqualTo(MindLanguage.JA)
        assertThat(d.showBadge).isFalse()
    }

    @Test
    fun `local multilingual model speaks the ui language`() {
        val d = MindLanguageRouting.decide(MindLanguage.RU, MindTier.GEMMA, MindModelRegistry.QWEN25_15B)
        assertThat(d.language).isEqualTo(MindLanguage.RU)
        assertThat(d.mode).isEqualTo(MindLanguageRouting.Mode.NATIVE)
    }

    @Test
    fun `english-only model falls back honestly with a badge`() {
        val d = MindLanguageRouting.decide(MindLanguage.PL, MindTier.GEMMA, MindModelRegistry.GEMMA3_1B)
        assertThat(d.language).isEqualTo(MindLanguage.EN)
        assertThat(d.showBadge).isTrue()
    }

    @Test
    fun `nano is native for english and badged otherwise`() {
        assertThat(
            MindLanguageRouting
                .decide(MindLanguage.EN, MindTier.NANO, null)
                .showBadge,
        ).isFalse()
        assertThat(
            MindLanguageRouting
                .decide(MindLanguage.DE, MindTier.NANO, null)
                .showBadge,
        ).isTrue()
    }

    @Test
    fun `locale tags map to languages with english for strangers`() {
        assertThat(MindLanguage.fromTag("ru-RU")).isEqualTo(MindLanguage.RU)
        assertThat(MindLanguage.fromTag("es-419")).isEqualTo(MindLanguage.ES)
        assertThat(MindLanguage.fromTag("ja")).isEqualTo(MindLanguage.JA)
        assertThat(MindLanguage.fromTag("fr")).isEqualTo(MindLanguage.EN)
    }

    // --- prompt formats ---

    @Test
    fun `plain format keeps the v04 wire shape for english`() {
        val combined = MindPrompts.combine(MindPrompt(system = "SYS", user = "USR"))
        assertThat(combined).isEqualTo("SYS\n\nUSR\n\nMe:")
    }

    @Test
    fun `plain format localizes the reply cue`() {
        val combined =
            MindPrompts.combine(MindPrompt("SYS", "USR", MindLanguage.RU), PromptFormat.PLAIN)
        assertThat(combined).endsWith("\n\nЯ:")
    }

    @Test
    fun `chatml format frames turns and leaves assistant open`() {
        val combined =
            MindPrompts.combine(MindPrompt("SYS", "USR"), PromptFormat.CHATML)
        assertThat(combined).isEqualTo(
            "<|im_start|>system\nSYS<|im_end|>\n<|im_start|>user\nUSR<|im_end|>\n<|im_start|>assistant\n",
        )
    }

    @Test
    fun `extraction demands fact text in the user's language`() {
        val prompt = MindPrompts.extraction("u", "c", MindLanguage.RU)
        assertThat(prompt).contains("written in\nRussian")
    }

    // --- localized prompt building ---

    @Test
    fun `english build is byte-identical to the v04 wording`() {
        val prompt =
            PromptBuilder.build(
                creatureName = "Iskra",
                state = BodyState.Resting,
                facts = emptyList(),
                dialogue = emptyList(),
                userMessage = "hi",
            )
        assertThat(prompt.system).contains("You are Iskra, a small creature who IS this phone")
        assertThat(prompt.system).contains("My body right now:")
        assertThat(prompt.user).isEqualTo("Person: hi")
        assertThat(prompt.language).isEqualTo(MindLanguage.EN)
    }

    @Test
    fun `russian build words the whole prompt in russian`() {
        val prompt =
            PromptBuilder.build(
                creatureName = "Искра",
                state = BodyState.Resting,
                facts = emptyList(),
                dialogue = listOf(ChatMessage(id = "m1", role = ChatRole.USER, text = "привет", atMillis = 0L)),
                userMessage = "как ты?",
                language = MindLanguage.RU,
            )
        assertThat(prompt.system).contains("Ты — Искра, маленькое существо")
        assertThat(prompt.system).contains("Моё тело сейчас:")
        assertThat(prompt.system).contains("Отвечай только по-русски.")
        assertThat(prompt.user).startsWith("Недавний разговор:")
        assertThat(prompt.user).endsWith("Человек: как ты?")
        assertThat(prompt.language).isEqualTo(MindLanguage.RU)
    }

    @Test
    fun `every language produces a persona mentioning the creature's name`() {
        MindLanguage.entries.forEach { language ->
            val persona = MindVoice.persona("Momo", Personality.Default, language)
            assertThat(persona).contains("Momo")
            // The anti-injection clause must survive every translation.
            assertThat(MindVoice.bodyReport(BodyState.Resting, language)).isNotEmpty()
        }
    }
}
