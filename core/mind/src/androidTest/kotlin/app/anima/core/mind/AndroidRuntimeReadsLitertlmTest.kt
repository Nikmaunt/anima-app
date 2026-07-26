package app.anima.core.mind

import androidx.test.platform.app.InstrumentationRegistry
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.common.truth.Truth.assertThat
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * The runtime matrix — v0.9 Phase E, generalising v0.8 Phase D.
 *
 * v0.8 asked one question of one cell (tasks-genai x Qwen2.5-1.5B q8 x
 * `.litertlm`), got a refusal, and the derived documents turned it into a
 * verdict about the engine. One cell cannot do that: a refusal there is
 * equally consistent with three different causes, and they imply three
 * different products.
 *
 *   container  — tasks-genai cannot read the `.litertlm` container at all
 *   tokenizer  — it reads the container but demands SentencePiece
 *   this file  — something about that one artifact
 *
 * So the model path is a parameter now, and the run script sweeps it. The
 * discriminating pair is the SAME model in TWO containers: Qwen ships both
 * `.litertlm` and `.task` from litert-community, so the tokenizer is held
 * constant while the container varies — which no Gemma cell could do,
 * since Gemma changes model, tokenizer and container at once.
 *
 *   gradlew :core:mind:connectedDebugAndroidTest \
 *     -Pandroid.testInstrumentationRunnerArguments.animaModel=/data/local/tmp/anima/x.task \
 *     --tests '*AndroidRuntimeReadsLitertlmTest*'
 *
 * WHAT THIS TEST STILL DOES NOT DO — read before quoting it anywhere: it
 * says NOTHING about speed. An x86_64 emulator on a desktop CPU shares no
 * property with an Exynos 2400. Checklist gate §0 stays wide open however
 * green this goes.
 *
 * Self-gating: the model is never in git and never downloaded here. The
 * test skips unless the file has been pushed, so default CI skips it.
 */
class AndroidRuntimeReadsLitertlmTest {
    private val modelPath: String
        get() =
            InstrumentationRegistry.getArguments().getString(ARG_MODEL)
                ?: DEFAULT_MODEL_PATH

    private fun requireModel(): File {
        val model = File(modelPath)
        assumeTrue(
            "gated: push a model to $modelPath to run this",
            model.isFile && model.length() > 0,
        )
        return model
    }

    /**
     * Reports the cell verbatim either way. A refusal is a result here, not
     * an accident, so the exact runtime text has to survive into the log —
     * paraphrasing it is how v0.8's over-generalisation happened.
     */
    private fun cell(
        engine: String,
        model: File,
        body: () -> String,
    ) {
        val head = "MATRIX|$engine|${model.name}|${model.length()}"
        val started = System.nanoTime()
        val reply =
            try {
                body()
            } catch (t: Throwable) {
                val ms = (System.nanoTime() - started) / 1_000_000
                println("$head|FAIL|${ms}ms|${t.javaClass.name}: ${t.message}")
                throw t
            }
        val ms = (System.nanoTime() - started) / 1_000_000
        println("$head|OK|${ms}ms|${reply.replace('\n', ' ')}")
        assertThat(reply.trim()).isNotEmpty()
    }

    @Test
    fun litertLmReadsTheFileAndAnswers() {
        val model = requireModel()
        cell("litertlm-android-0.14.0", model) {
            val reply = StringBuilder()
            Engine(
                EngineConfig(
                    modelPath = model.absolutePath,
                    // ADR-020: CPU is the only backend this project constructs.
                    backend = Backend.CPU(),
                ),
            ).use { engine ->
                engine.initialize()
                engine.createConversation().use { conversation ->
                    conversation
                        .sendMessage(PROMPT)
                        .contents.contents
                        .filterIsInstance<Content.Text>()
                        .forEach { reply.append(it.text) }
                }
            }
            reply.toString()
        }
    }

    @Test
    fun tasksGenaiReadsTheFileAndAnswers() {
        val model = requireModel()
        cell("tasks-genai-0.10.35", model) {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val options =
                LlmInference.LlmInferenceOptions
                    .builder()
                    .setModelPath(model.absolutePath)
                    .setMaxTokens(MAX_TOKENS)
                    .build()
            LlmInference.createFromOptions(context, options).use { llm ->
                llm.generateResponse(PROMPT).orEmpty()
            }
        }
    }

    private companion object {
        const val ARG_MODEL = "animaModel"
        const val DEFAULT_MODEL_PATH = "/data/local/tmp/anima/model.litertlm"
        const val PROMPT = "Say hello in one short sentence."
        const val MAX_TOKENS = 512
    }
}
