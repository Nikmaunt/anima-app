package app.anima.core.mind

import android.content.Context
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.anima.core.model.BodyState
import app.anima.core.model.InstalledMindModel
import app.anima.core.model.MindEvent
import app.anima.core.model.MindLanguage
import app.anima.core.model.MindModelLocator
import app.anima.core.model.MindModelRegistry
import app.anima.core.model.PromptBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Phase 1C bake-off harness (ADR-017): runs a fixed prompt set — persona
 * chat in all six product languages plus fact extraction — against EVERY
 * model file pushed to the device, and writes a JSON report with latency
 * and reply excerpts. On the S24 this turns "which model should the pack
 * carry" into one adb command per candidate:
 *
 *   adb push qwen2.5-1.5b-q8.task \
 *     /sdcard/Android/data/app.anima.core.mind.test/files/bakeoff/
 *   gradlew :core:mind:atd34DebugAndroidTest   (or connectedDebugAndroidTest)
 *   adb pull /sdcard/Android/data/app.anima.core.mind.test/files/bakeoff-report.json
 *
 * With no pushed models the test SKIPS (never fails): the GMD emulator has
 * no weights, and an honest skip beats a fake green.
 */
@RunWith(AndroidJUnit4::class)
class MindBakeoffHarness {
    private class FixedLocator(
        model: InstalledMindModel?,
    ) : MindModelLocator {
        override val installed: StateFlow<InstalledMindModel?> = MutableStateFlow(model)
    }

    private data class ChatProbe(
        val language: MindLanguage,
        val userMessage: String,
    )

    /** Same question in each product language — comparable across models. */
    private val chatProbes =
        listOf(
            ChatProbe(MindLanguage.EN, "How do you feel right now?"),
            ChatProbe(MindLanguage.RU, "Как ты себя сейчас чувствуешь?"),
            ChatProbe(MindLanguage.PL, "Jak się teraz czujesz?"),
            ChatProbe(MindLanguage.DE, "Wie fühlst du dich gerade?"),
            ChatProbe(MindLanguage.ES, "¿Cómo te sientes ahora mismo?"),
            ChatProbe(MindLanguage.JA, "いま、どんな気分？"),
        )

    @Test
    fun bakeoff() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val modelsDir = File(context.getExternalFilesDir(null), "bakeoff")
        modelsDir.mkdirs()
        val models =
            modelsDir
                .listFiles()
                ?.filter { it.isFile && it.extension in setOf("task", "litertlm") }
                .orEmpty()
                .sortedBy { it.name }
        assumeTrue(
            "No model files under ${modelsDir.absolutePath} — adb push .task/.litertlm files to run the bake-off.",
            models.isNotEmpty(),
        )

        val report =
            buildJsonArray {
                models.forEach { file -> add(measureModel(context, file)) }
            }
        val out = File(context.getExternalFilesDir(null), "bakeoff-report.json")
        out.writeText(report.toString())
        // The harness's own assertion is honesty: the report exists and
        // covered every pushed model.
        check(out.length() > 0L)
    }

    private fun measureModel(
        context: Context,
        file: File,
    ) = buildJsonObject {
        val installed = InstalledMindModel(file.name, file.length(), file.absolutePath)
        val spec = MindModelRegistry.specFor(installed)
        val engine = GemmaMindEngine(context, FixedLocator(installed))
        put("file", file.name)
        put("specId", spec.id)
        put("sizeBytes", file.length())
        put(
            "chat",
            buildJsonArray {
                chatProbes.forEach { probe ->
                    add(
                        runProbe(engine, spec.promptCharBudget, probe),
                    )
                }
            },
        )
        put(
            "extraction",
            runBlocking {
                val started = SystemClock.elapsedRealtime()
                val candidates =
                    runCatching {
                        withTimeout(PROBE_TIMEOUT_MS) {
                            engine.extractFactCandidates(
                                userText = "My name is Nick and I love rainy mornings.",
                                creatureText = "Rainy mornings! I will remember that.",
                                language = MindLanguage.EN,
                            )
                        }
                    }.getOrDefault(emptyList())
                buildJsonObject {
                    put("candidates", candidates.size)
                    put("totalMs", SystemClock.elapsedRealtime() - started)
                }
            },
        )
        runBlocking { engine.releaseResources() }
    }

    private fun runProbe(
        engine: GemmaMindEngine,
        budgetChars: Int,
        probe: ChatProbe,
    ) = runBlocking {
        val prompt =
            PromptBuilder.build(
                creatureName = "Iskra",
                state = BodyState.Resting,
                facts = emptyList(),
                dialogue = emptyList(),
                userMessage = probe.userMessage,
                budgetChars = budgetChars,
                language = probe.language,
            )
        val started = SystemClock.elapsedRealtime()
        var firstChunkMs = -1L
        val full = StringBuilder()
        var failed = false
        runCatching {
            withTimeout(PROBE_TIMEOUT_MS) {
                engine.reply(prompt).collect { event ->
                    when (event) {
                        is MindEvent.Chunk -> {
                            if (firstChunkMs < 0) firstChunkMs = SystemClock.elapsedRealtime() - started
                            full.append(event.text)
                        }
                        is MindEvent.Done -> Unit
                        is MindEvent.Failed -> failed = true
                    }
                }
            }
        }.onFailure { failed = true }
        val totalMs = SystemClock.elapsedRealtime() - started
        buildJsonObject {
            put("language", probe.language.tag)
            put("failed", failed)
            put("firstChunkMs", firstChunkMs)
            put("totalMs", totalMs)
            put("chars", full.length)
            put("excerpt", full.toString().take(EXCERPT_CHARS))
        }
    }

    private companion object {
        /** First reply includes a cold model load — allow minutes, honestly. */
        const val PROBE_TIMEOUT_MS = 180_000L
        const val EXCERPT_CHARS = 240
    }
}
