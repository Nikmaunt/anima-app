package app.anima.core.mind

import app.anima.core.model.FactCandidate
import app.anima.core.model.MindEngine
import app.anima.core.model.MindEvent
import app.anima.core.model.MindPrompt
import app.anima.core.model.MindStatus

/**
 * Deterministic fake (hermes-app FakeProvider pattern): reads no clock, no
 * randomness. A scripted handler receives (prompt, callIndex) so tests can
 * script attempt-1 vs attempt-2; streaming splits the reply word-by-word so
 * chat streaming UIs are exercised for real.
 */
class FakeMindEngine(
    private val fixedStatus: MindStatus = MindStatus.READY,
    private val extraction: List<FactCandidate> = emptyList(),
    private val handler: (prompt: MindPrompt, callIndex: Int) -> String = { _, _ ->
        "I felt that tap all the way down my battery."
    },
) : MindEngine {
    private var calls = 0

    override suspend fun status(): MindStatus = fixedStatus

    override suspend fun requestDownload(): Boolean = fixedStatus != MindStatus.ASLEEP

    override fun reply(prompt: MindPrompt): kotlinx.coroutines.flow.Flow<MindEvent> =
        kotlinx.coroutines.flow.flow {
            val text = handler(prompt, calls++)
            val words = text.split(" ")
            words.forEachIndexed { index, word ->
                emit(MindEvent.Chunk(if (index == 0) word else " $word"))
            }
            emit(MindEvent.Done(text))
        }

    override suspend fun extractFactCandidates(
        userText: String,
        creatureText: String,
        language: app.anima.core.model.MindLanguage,
    ): List<FactCandidate> = extraction
}
