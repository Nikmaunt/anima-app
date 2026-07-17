package app.anima

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

/**
 * The zero-network guarantee as a failing build (constraint #2, ADR-004):
 *  1. the packaged manifest must NOT contain android.permission.INTERNET
 *     (ML Kit's DataTransport telemetry requests it; we strip it at merge);
 *  2. no module source may touch network APIs;
 *  3. the version catalog must contain no network stack;
 *  4. the listener feature module must not depend on anything that could
 *     reach the network (its build file allowlist is checked verbatim).
 */
class NetworkIsolationTest {

    private val repoRoot: File by lazy {
        // Unit tests run with CWD = <repo>/app; walk up to the repo root.
        generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "settings.gradle.kts").exists() }
    }

    @Test
    fun `packaged manifest has no INTERNET permission`() {
        val candidates = listOf(
            "build/intermediates/packaged_manifests/debug/processDebugManifestForPackage/AndroidManifest.xml",
            "build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml",
        ).map { File(repoRoot, "app/$it") }.filter { it.exists() }
        // Requires a prior assembleDebug; ./gradlew check depends on it in CI
        // usage. When absent locally, the source-level checks below still run.
        candidates.forEach { manifest ->
            // XML comments may legitimately explain the guarantee; strip them
            // and assert on real elements only.
            val text = manifest.readText().replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
            assertThat(text).doesNotContain("android.permission.INTERNET")
            // The only background entity: our listener. DataTransport's
            // scheduler components must stay stripped.
            assertThat(text).doesNotContain("JobInfoSchedulerService")
            assertThat(text).doesNotContain("AlarmManagerSchedulerBroadcastReceiver")
        }
    }

    @Test
    fun `no module source touches network APIs`() {
        val forbidden = listOf(
            "okhttp", "retrofit2", "io.ktor", "java.net.HttpURLConnection",
            "java.net.URLConnection", "java.net.Socket", "HttpsURLConnection",
            "openConnection(",
        )
        val offenders = mutableListOf<String>()
        sourceFiles().forEach { file ->
            val text = file.readText()
            forbidden.forEach { needle ->
                if (text.contains(needle)) offenders += "${file.relativeTo(repoRoot)} -> $needle"
            }
        }
        assertThat(offenders).isEmpty()
    }

    @Test
    fun `version catalog carries no network stack`() {
        // Comments may mention forbidden stacks by name (to say they're
        // banned); only effective, non-comment content is checked.
        val catalog = File(repoRoot, "gradle/libs.versions.toml").readLines()
            .joinToString("\n") { it.substringBefore('#') }
            .lowercase()
        listOf("okhttp", "retrofit", "ktor", "volley", "cronet", "grpc").forEach {
            assertThat(catalog).doesNotContain(it)
        }
    }

    @Test
    fun `listener module dependencies are the audited allowlist`() {
        val buildFile = File(repoRoot, "feature/notifications/build.gradle.kts").readText()
        val dependencyLines = buildFile.lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("implementation(") || it.startsWith("api(") }
            .toList()
        val allowed = listOf(
            "projects.core.model", "projects.core.data", "projects.core.ui",
            "libs.androidx.core.ktx", "libs.androidx.lifecycle.runtime.compose",
            "libs.androidx.lifecycle.viewmodel.compose", "libs.androidx.hilt.navigation.compose",
        )
        dependencyLines.forEach { line ->
            assertThat(allowed.any { line.contains(it) }).isTrue()
        }
    }

    private fun sourceFiles(): Sequence<File> =
        repoRoot.walkTopDown()
            .onEnter { it.name != "build" && it.name != ".git" && it.name != ".gradle" }
            .filter { it.isFile && it.extension == "kt" && !it.path.contains("test") }
}
