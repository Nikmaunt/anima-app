package app.anima

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

/**
 * The network guarantee as a failing build, v2 (ADR-004 as amended by
 * ADR-005). What changed against v0.1, per the fresh audit (docs/audit-v01.md):
 *
 *  1. INTERNET now legitimately exists in the merged manifest — but it must
 *     originate from exactly one module manifest, :core:model-delivery.
 *  2. The merged-manifest check can no longer pass vacuously: if no build
 *     output exists the test FAILS and tells you to run assembleDebug.
 *  3. The dependency grep now covers every build.gradle.kts in the repo —
 *     a hardcoded okhttp coordinate in any module fails the build.
 *  4. Source-level network APIs stay banned everywhere EXCEPT
 *     core/model-delivery (the single sanctioned exception).
 */
class NetworkIsolationTest {
    private val repoRoot: File by lazy {
        // Unit tests run with CWD = <repo>/app; walk up to the repo root.
        generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "settings.gradle.kts").exists() }
    }

    @Test
    fun `merged manifest exists and carries INTERNET only via model-delivery`() {
        val candidates =
            listOf(
                "build/intermediates/packaged_manifests/debug/processDebugManifestForPackage/AndroidManifest.xml",
                "build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml",
            ).map { File(repoRoot, "app/$it") }.filter { it.exists() }
        // v0.1 gap closed: absence of build output is a FAILURE, not a pass.
        assertWithMessage(
            "No merged manifest found — run `gradlew assembleDebug` before `test`; " +
                "this check is meaningless without it and refuses to pretend otherwise.",
        ).that(candidates).isNotEmpty()
        candidates.forEach { manifest ->
            val text = manifest.readText().replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
            // INTERNET is expected now (model delivery) — but the telemetry
            // components must stay stripped.
            assertThat(text).contains("android.permission.INTERNET")
            assertThat(text).doesNotContain("JobInfoSchedulerService")
            assertThat(text).doesNotContain("AlarmManagerSchedulerBroadcastReceiver")
            assertThat(text).doesNotContain("TransportBackendDiscovery")
        }
    }

    @Test
    fun `INTERNET is declared by exactly one module manifest - model-delivery`() {
        val declaring =
            repoRoot
                .walkTopDown()
                .onEnter { it.name !in setOf("build", ".git", ".gradle") }
                .filter { it.isFile && it.name == "AndroidManifest.xml" }
                .filter { manifest ->
                    val text = manifest.readText().replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
                    text.contains("android.permission.INTERNET")
                }.map { it.relativeTo(repoRoot).path.replace('\\', '/') }
                .toList()
        assertThat(declaring).containsExactly("core/model-delivery/src/main/AndroidManifest.xml")
    }

    @Test
    fun `no module source touches network APIs except model-delivery`() {
        val forbidden =
            listOf(
                "okhttp",
                "retrofit2",
                "io.ktor",
                "java.net.HttpURLConnection",
                "java.net.URLConnection",
                "java.net.Socket",
                "HttpsURLConnection",
                "openConnection(",
            )
        val offenders = mutableListOf<String>()
        sourceFiles()
            .filterNot { it.path.replace('\\', '/').contains("core/model-delivery/") }
            .forEach { file ->
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
        val catalog =
            File(repoRoot, "gradle/libs.versions.toml")
                .readLines()
                .joinToString("\n") { it.substringBefore('#') }
                .lowercase()
        listOf("okhttp", "retrofit", "ktor", "volley", "cronet", "grpc").forEach {
            assertThat(catalog).doesNotContain(it)
        }
    }

    @Test
    fun `no module build file adds a network dependency by coordinate`() {
        // v0.1 gap closed: the catalog test alone missed hardcoded
        // implementation("com.squareup.okhttp3:...") lines in module builds.
        val needles = listOf("okhttp", "retrofit", "io.ktor", "volley", "cronet", "grpc")
        val offenders = mutableListOf<String>()
        repoRoot
            .walkTopDown()
            .onEnter { it.name !in setOf("build", ".git", ".gradle") }
            .filter { it.isFile && it.name.endsWith(".gradle.kts") }
            .forEach { buildFile ->
                val effective =
                    buildFile
                        .readLines()
                        .joinToString("\n") { it.substringBefore("//") }
                        .lowercase()
                needles.forEach { needle ->
                    if (effective.contains(needle)) {
                        offenders += "${buildFile.relativeTo(repoRoot)} -> $needle"
                    }
                }
            }
        assertThat(offenders).isEmpty()
    }

    @Test
    fun `listener module dependencies are the audited allowlist`() {
        val buildFile = File(repoRoot, "feature/notifications/build.gradle.kts").readText()
        val dependencyLines =
            buildFile
                .lineSequence()
                .map { it.trim() }
                .filter { it.startsWith("implementation(") || it.startsWith("api(") }
                .toList()
        val allowed =
            listOf(
                "projects.core.model",
                "projects.core.data",
                "projects.core.ui",
                "libs.androidx.core.ktx",
                "libs.androidx.lifecycle.runtime.compose",
                "libs.androidx.lifecycle.viewmodel.compose",
                "libs.androidx.hilt.navigation.compose",
            )
        dependencyLines.forEach { line ->
            assertWithMessage("unexpected dependency in listener module: $line")
                .that(allowed.any { line.contains(it) })
                .isTrue()
        }
    }

    private fun sourceFiles(): Sequence<File> =
        repoRoot
            .walkTopDown()
            .onEnter { it.name != "build" && it.name != ".git" && it.name != ".gradle" }
            .filter { it.isFile && it.extension == "kt" && !it.path.contains("test") }
}
