package app.anima.tools.smoke

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * ADR-020 §5 — the version-pair proof: litertlm-jvm as pinned in the
 * catalog actually initializes on this host's CPU and answers one prompt on
 * the smallest ready-made .litertlm from litert-community (verified
 * 2026-07-24, freshness-2026-07 §A). The model lands in the system temp dir
 * and is reused across runs; override the location with ANIMA_JVM_LLM_MODEL
 * (an already-downloaded file skips the network entirely).
 *
 * Deliberately NOT in the default loop: without ANIMA_JVM_LLM_SMOKE=1 the
 * test skips immediately.
 */
class LiteRtLmJvmSmokeTest {
    @Test
    fun `engine initializes on CPU and answers one prompt`() {
        assumeTrue(
            "gated: set ANIMA_JVM_LLM_SMOKE=1 to run the smoke",
            System.getenv("ANIMA_JVM_LLM_SMOKE") == "1",
        )
        val model = obtainModel()
        println("smoke: model at ${model.absolutePath} (${model.length()} bytes)")

        val reply = StringBuilder()
        Engine(
            EngineConfig(
                modelPath = model.absolutePath,
                // ADR-020: CPU only — desktop GPU is unreliable (#1857/#1748).
                backend = Backend.CPU(),
            ),
        ).use { engine ->
            engine.initialize()
            engine.createConversation().use { conversation ->
                val message = conversation.sendMessage("Say hello in one short sentence.")
                message.contents.contents
                    .filterIsInstance<Content.Text>()
                    .forEach { reply.append(it.text) }
            }
        }
        println("smoke: reply = \"$reply\"")
        assertThat(reply.toString().trim()).isNotEmpty()
    }

    private fun obtainModel(): File {
        System.getenv("ANIMA_JVM_LLM_MODEL")?.let { override ->
            val file = File(override)
            require(file.isFile) { "ANIMA_JVM_LLM_MODEL points to no file: $override" }
            return file
        }
        val cache =
            File(
                System.getenv("ANIMA_JVM_LLM_CACHE")
                    ?: System.getProperty("java.io.tmpdir"),
                "anima-llm-smoke",
            )
        val target = File(cache, MODEL_FILE)
        if (target.isFile && target.length() > 0) return target
        cache.mkdirs()
        println("smoke: downloading $MODEL_URL → $target (~1.6 GiB, once)")
        URI(MODEL_URL).toURL().openStream().use { stream ->
            Files.copy(stream, target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        return target
    }

    private companion object {
        /**
         * The PRODUCT's pack-default model (ADR-017/018) in the official
         * litert-community q8 build — 1.59 GB but ungated Apache-2.0. The
         * smaller gemma3-270m .litertlm exists yet its HF repo is
         * license-gated and yields 401 on anonymous download (the v0.2 HF
         * gate lesson, re-proven on the host in this session's first run).
         * ANIMA_JVM_LLM_CACHE relocates the download (small system drives);
         * ANIMA_JVM_LLM_MODEL points at any local .litertlm and skips the
         * network entirely.
         */
        const val MODEL_FILE = "Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm"
        const val MODEL_URL =
            "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/$MODEL_FILE"
    }
}
