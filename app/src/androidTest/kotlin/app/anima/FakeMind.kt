package app.anima

import app.anima.core.model.FactCandidate
import app.anima.core.model.InstalledMindModel
import app.anima.core.model.LocalMindEngine
import app.anima.core.model.MindEvent
import app.anima.core.model.MindInventory
import app.anima.core.model.MindLanguage
import app.anima.core.model.MindPrompt
import app.anima.core.model.MindSnapshot
import app.anima.core.model.MindStatus
import app.anima.core.model.MindTier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Deterministic fake for the day-in-life scenario (androidTest copy of
 * core/mind's test-source FakeMindEngine, extended to serve every mind-facing
 * binding at once). No clock, no randomness: the reply is a fixed string,
 * streamed word by word so the chat streaming UI is exercised for real.
 */
class FakeMind :
    LocalMindEngine,
    MindInventory {
    override suspend fun status(): MindStatus = MindStatus.READY

    override suspend fun requestDownload(): Boolean = true

    override fun reply(prompt: MindPrompt): Flow<MindEvent> =
        flow {
            val words = REPLY.split(" ")
            words.forEachIndexed { index, word ->
                emit(MindEvent.Chunk(if (index == 0) word else " $word"))
            }
            emit(MindEvent.Done(REPLY))
        }

    override suspend fun extractFactCandidates(
        userText: String,
        creatureText: String,
        language: MindLanguage,
    ): List<FactCandidate> = emptyList()

    override suspend fun snapshot(): MindSnapshot =
        MindSnapshot(
            activeTier = MindTier.GEMMA,
            nano = MindStatus.ASLEEP,
            // A fictitious installed model: keeps every inventory-driven UI on
            // its "mind present" path (HomeScreen itself keys off status()).
            gemmaModel =
                InstalledMindModel(
                    fileName = "fake-gemma.task",
                    sizeBytes = 1L,
                    path = "/dev/null/fake-gemma.task",
                ),
        )

    companion object {
        const val REPLY = "I felt that tap all the way down my battery."
    }
}
