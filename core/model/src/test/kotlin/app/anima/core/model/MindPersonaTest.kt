package app.anima.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * What the persona TEXT must hold in every language (v1.0 Phase P, ADR-023).
 *
 * These are cheap string assertions and they are worth more than they look:
 * the v0.9 wording failed on a phone in ways nobody could see from the code,
 * and the fixes are one careless edit away from being undone. The acceptance
 * harness (`:tools:litertlm-smoke`, needs 1.6 GB of weights) proves what the
 * MODEL does with the text; this proves what the text says, on every machine,
 * in the ordinary loop.
 */
class MindPersonaTest {
    private fun persona(language: MindLanguage) = MindVoice.persona("Мика", Personality.Default, language)

    /**
     * The measured lesson of Phase P: a negated word is still the word. v1
     * said "you are never an assistant" in all six languages and the model
     * answered "How can I assist you today?" — so the term is gone from the
     * prompt entirely and the invariant is enforced by the acceptance check
     * `NOT_AN_ASSISTANT` instead.
     */
    @Test
    fun `no language mentions the word this creature must never use`() {
        val forbidden =
            listOf(
                "assistant",
                "asystent",
                "assistent",
                "asistente",
                "アシスタント",
                "ассистент",
                "ai ",
                "artificial intelligence",
                "языковая модель",
            )
        MindLanguage.entries.forEach { language ->
            val text = persona(language).lowercase()
            forbidden.forEach { word ->
                assertThat(text).doesNotContain(word)
            }
        }
    }

    @Test
    fun `every language tells the creature to answer in that language`() {
        val marker =
            mapOf(
                MindLanguage.EN to "Speak English",
                MindLanguage.RU to "Говори по-русски",
                MindLanguage.PL to "Mów po polsku",
                MindLanguage.DE to "Sprich Deutsch",
                MindLanguage.ES to "Habla español",
                MindLanguage.JA to "日本語で話してね",
            )
        MindLanguage.entries.forEach { language ->
            assertThat(persona(language)).contains(marker.getValue(language))
        }
    }

    /**
     * The soul contract, stated as an action. Without this the model falls
     * back on its instruct-tuning and tells the person their data was saved
     * when nothing was saved (v0.9 bench, case 6).
     */
    @Test
    fun `every language makes the creature ask for confirmation before keeping a fact`() {
        val confirm =
            mapOf(
                MindLanguage.EN to "Confirm",
                MindLanguage.RU to "Подтвердить",
                MindLanguage.PL to "Potwierdź",
                MindLanguage.DE to "Bestätigen",
                MindLanguage.ES to "Confirmar",
                MindLanguage.JA to "確認",
            )
        MindLanguage.entries.forEach { language ->
            assertThat(persona(language)).contains(confirm.getValue(language))
        }
    }

    @Test
    fun `every language keeps the body metaphor and the creature's name`() {
        MindLanguage.entries.forEach { language ->
            val text = persona(language)
            assertThat(text).contains("Мика")
            assertThat(text.length).isGreaterThan(MIN_PERSONA_CHARS)
        }
    }

    /**
     * A small model follows a short prompt more reliably than a long one, and
     * the persona is only part of what PromptBuilder sends. This is a ceiling,
     * not a target: cross it and the body report plus facts start to crowd out
     * the character.
     */
    @Test
    fun `no language turns the persona into an essay`() {
        MindLanguage.entries.forEach { language ->
            assertThat(persona(language).length).isLessThan(MAX_PERSONA_CHARS)
        }
    }

    @Test
    fun `the version string is quoted by whatever measures the prompt`() {
        assertThat(MindPersona.VERSION).isNotEmpty()
        assertThat(MindPersona.VERSION).startsWith("persona-v")
    }

    private companion object {
        const val MIN_PERSONA_CHARS = 300
        const val MAX_PERSONA_CHARS = 1700
    }
}
