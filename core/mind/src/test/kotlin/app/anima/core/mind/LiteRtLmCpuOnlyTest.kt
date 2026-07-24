package app.anima.core.mind

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

/**
 * ADR-020 as a failing build, not a comment: the LiteRT-LM engine may only
 * ever construct the CPU backend. The Samsung GPU issue cluster
 * (freshness-2026-07 §A) is open and the runtime has no GPU→CPU fallback;
 * the day GPU becomes viable, this test is revised by a NEW ADR, not by an
 * edit that quietly slips `Backend.GPU()` in.
 */
class LiteRtLmCpuOnlyTest {
    private val repoRoot: File by lazy {
        generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "settings.gradle.kts").exists() }
    }

    private fun debugSources(): List<File> =
        File(repoRoot, "core/mind/src/debug")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()

    @Test
    fun `debug source set exists and contains the engine`() {
        assertThat(debugSources().map { it.name }).contains("LiteRtLmMindEngine.kt")
    }

    @Test
    fun `the engine constructs the CPU backend`() {
        val engine =
            debugSources().first { it.name == "LiteRtLmMindEngine.kt" }.readText()
        assertThat(engine).contains("Backend.CPU(")
    }

    @Test
    fun `no debug source spells any other backend`() {
        val banned = listOf("Backend.GPU", "Backend.NPU", "GPU(", "NPU(")
        debugSources().forEach { file ->
            val text = file.readText()
            banned.forEach { needle ->
                assertWithMessage("${file.name} must not reference $needle (ADR-020: CPU only)")
                    .that(text.contains(needle))
                    .isFalse()
            }
        }
    }
}
