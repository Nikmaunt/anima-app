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
 * v0.8 Phase D — link (2) of risk 6.2: does an ANDROID runtime actually
 * read the `.litertlm` file we intend to ship in the pack? Until v0.8 that
 * question could only be asked on the owner's S24, because nobody had
 * checked whether the runtimes ship an x86_64 slice. They do:
 *
 *   tasks-genai 0.10.35   jni/{arm64-v8a, armeabi-v7a, x86, x86_64}
 *   litertlm-android 0.14.0 jni/{arm64-v8a, x86_64}
 *
 * so both engines can be exercised on an x86_64 emulator, and this test
 * runs BOTH against the SAME file — the first direct comparison of the two
 * runtimes on one artifact and one Android.
 *
 * WHAT THIS TEST DOES NOT DO — read this before quoting it anywhere:
 * it says NOTHING about speed. An x86_64 emulator on a desktop CPU shares
 * no property with an Exynos 2400. Checklist gate §0 (speed and quality on
 * the real phone) stays wide open no matter how green this goes.
 *
 * Self-gating: the model is never in git and never downloaded here. The
 * test skips unless the file has been pushed to the device, so the default
 * CI run — which pushes nothing — skips it instantly:
 *
 *   adb push <model>.litertlm /data/local/tmp/anima/model.litertlm
 *   gradlew :core:mind:connectedDebugAndroidTest \
 *     --tests '*AndroidRuntimeReadsLitertlmTest*'
 */
class AndroidRuntimeReadsLitertlmTest {
    private val model = File(DEVICE_MODEL_PATH)

    private fun requireModel() {
        assumeTrue(
            "gated: push a .litertlm to $DEVICE_MODEL_PATH to run this",
            model.isFile && model.length() > 0,
        )
        println("phase-d: model ${model.absolutePath} (${model.length()} bytes)")
    }

    @Test
    fun litertLmReadsTheFileAndAnswers() {
        requireModel()
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
        println("phase-d: LiteRT-LM reply = \"$reply\"")
        assertThat(reply.toString().trim()).isNotEmpty()
    }

    @Test
    fun tasksGenaiReadsTheFileAndAnswers() {
        requireModel()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val options =
            LlmInference.LlmInferenceOptions
                .builder()
                .setModelPath(model.absolutePath)
                .setMaxTokens(MAX_TOKENS)
                .build()
        val reply =
            LlmInference.createFromOptions(context, options).use { llm ->
                llm.generateResponse(PROMPT)
            }
        println("phase-d: tasks-genai reply = \"$reply\"")
        assertThat(reply.orEmpty().trim()).isNotEmpty()
    }

    private companion object {
        const val DEVICE_MODEL_PATH = "/data/local/tmp/anima/model.litertlm"
        const val PROMPT = "Say hello in one short sentence."
        const val MAX_TOKENS = 512
    }
}
