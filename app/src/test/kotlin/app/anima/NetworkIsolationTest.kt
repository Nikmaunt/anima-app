package app.anima

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

/**
 * The network guarantee as a failing build, v4 (ADR-004 as amended by
 * ADR-005 and ADR-011; audit-v03 F6/F8/F1). What changed against v3:
 *
 *  1. The merged-manifest check now also covers the RELEASE variant when
 *     its build output exists (debug remains mandatory).
 *  2. The forbidden-API needle list grew: openStream, SocketChannel,
 *     DatagramSocket, ServerSocket, WebView, DownloadManager.
 *  3. The merged permission budget is pinned as an exact allowlist —
 *     library-merged additions (WorkManager, AICore) are documented in
 *     ADR-004 and any NEW permission fails this build.
 *  4. Digest guard (audit-v03 F1): the notifications module must speak to
 *     the mind through LocalMindEngine only — notification titles must
 *     never reach the cloud tier.
 */
class NetworkIsolationTest {
    private val networkModules =
        listOf(
            "core/model-delivery/src/main/AndroidManifest.xml",
            "core/cloud-mind/src/main/AndroidManifest.xml",
        )

    private val repoRoot: File by lazy {
        // Unit tests run with CWD = <repo>/app; walk up to the repo root.
        generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "settings.gradle.kts").exists() }
    }

    private fun mergedManifests(): List<File> {
        val debug =
            listOf(
                "build/intermediates/packaged_manifests/debug/processDebugManifestForPackage/AndroidManifest.xml",
                "build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml",
            ).map { File(repoRoot, "app/$it") }.filter { it.exists() }
        // v0.1 gap closed: absence of the debug output is a FAILURE, not a pass.
        assertWithMessage(
            "No merged manifest found — run `gradlew assembleDebug` before `test`; " +
                "this check is meaningless without it and refuses to pretend otherwise.",
        ).that(debug).isNotEmpty()
        // v4: the release output is checked whenever it has been built.
        val release =
            listOf(
                "build/intermediates/packaged_manifests/release/processReleaseManifestForPackage/AndroidManifest.xml",
                "build/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml",
            ).map { File(repoRoot, "app/$it") }.filter { it.exists() }
        return debug + release
    }

    @Test
    fun `merged manifest exists and carries INTERNET only via model-delivery`() {
        mergedManifests().forEach { manifest ->
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
    fun `merged permission budget is exactly the documented set`() {
        // ADR-004 addendum (audit-v03 F8): the app's own three permissions
        // plus the library-merged residue, in full. Anything new fails.
        val allowed =
            setOf(
                "android.permission.INTERNET",
                "android.permission.ACCESS_NETWORK_STATE",
                "android.permission.VIBRATE",
                "android.permission.FOREGROUND_SERVICE",
                "android.permission.FOREGROUND_SERVICE_DATA_SYNC",
                "android.permission.WAKE_LOCK",
                "android.permission.RECEIVE_BOOT_COMPLETED",
                "com.google.android.apps.aicore.service.BIND_SERVICE",
                // androidx.core synthesizes this app-private permission for
                // every app to protect non-exported dynamic receivers.
                "app.anima.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
            )
        mergedManifests().forEach { manifest ->
            val text = manifest.readText().replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
            val declared =
                Regex("<uses-permission[^>]*android:name=\"([^\"]+)\"")
                    .findAll(text)
                    .map { it.groupValues[1] }
                    .toSet()
            assertWithMessage("undocumented permissions in ${manifest.name}")
                .that(declared - allowed)
                .isEmpty()
            assertThat(declared).contains("android.permission.INTERNET")
            assertThat(declared).contains("android.permission.VIBRATE")
        }
    }

    @Test
    fun `notification digest speaks only to the local mind`() {
        // audit-v03 F1: the digest prompt carries notification titles; the
        // DI type LocalMindEngine makes the cloud tier unreachable. This
        // static tripwire fails if the module ever goes back to the full
        // MindEngine or reaches for the cloud backend directly.
        val moduleSrc = File(repoRoot, "feature/notifications/src/main")
        val viewModel = File(moduleSrc, "kotlin/app/anima/feature/notifications/NotificationsViewModel.kt").readText()
        assertThat(viewModel).contains("LocalMindEngine")
        assertThat(viewModel).doesNotContain("val mind: MindEngine")
        moduleSrc
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                assertWithMessage("${file.name} must not touch the cloud backend")
                    .that(file.readText().contains("CloudMindBackend"))
                    .isFalse()
            }
    }

    @Test
    fun `INTERNET is declared by exactly two module manifests - the sanctioned pair`() {
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
        assertThat(declaring).containsExactlyElementsIn(networkModules)
    }

    @Test
    fun `no module source touches network APIs except the sanctioned pair`() {
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
                // v4 (audit-v03 F6): the quieter ways out of the sandbox.
                "openStream(",
                "SocketChannel",
                "DatagramSocket",
                "ServerSocket",
                "WebView",
                "DownloadManager",
            )
        val offenders = mutableListOf<String>()
        sourceFiles()
            .filterNot { file ->
                val path = file.path.replace('\\', '/')
                path.contains("core/model-delivery/") || path.contains("core/cloud-mind/")
            }.forEach { file ->
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

    @Test
    fun `cloud-mind module dependencies are the audited allowlist`() {
        val buildFile = File(repoRoot, "core/cloud-mind/build.gradle.kts").readText()
        val dependencyLines =
            buildFile
                .lineSequence()
                .map { it.trim() }
                .filter { it.startsWith("implementation(") || it.startsWith("api(") }
                .toList()
        val allowed =
            listOf(
                "projects.core.model",
                "libs.androidx.core.ktx",
                "libs.kotlinx.coroutines.android",
                "libs.kotlinx.serialization.json",
                "libs.androidx.datastore.preferences",
            )
        dependencyLines.forEach { line ->
            assertWithMessage("unexpected dependency in cloud-mind module: $line")
                .that(allowed.any { line.contains(it) })
                .isTrue()
        }
    }

    @Test
    fun `cloud-mind never logs - the key and the user's words stay out of logcat`() {
        val offenders = mutableListOf<String>()
        File(repoRoot, "core/cloud-mind/src/main")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val text = file.readText()
                val banned =
                    listOf("android.util.Log", "Log.d(", "Log.i(", "Log.w(", "Log.e(", "println(", "printStackTrace(")
                banned.forEach { needle ->
                    if (text.contains(needle)) offenders += "${file.relativeTo(repoRoot)} -> $needle"
                }
            }
        assertThat(offenders).isEmpty()
    }

    @Test
    fun `soul export never touches the cloud key`() {
        // The BYOK key lives in cloud.key (noBackupFilesDir); the export
        // reads the DB only. Neither the backup codec nor any UI surface may
        // reference the key file or the vault.
        val offenders = mutableListOf<String>()
        sourceFiles()
            .filterNot { it.path.replace('\\', '/').contains("core/cloud-mind/") }
            .forEach { file ->
                val text = file.readText()
                listOf("cloud.key", "CloudKeyVault").forEach { needle ->
                    if (text.contains(needle)) offenders += "${file.relativeTo(repoRoot)} -> $needle"
                }
            }
        assertThat(offenders).isEmpty()
    }

    private fun sourceFiles(): Sequence<File> =
        repoRoot
            .walkTopDown()
            .onEnter { it.name != "build" && it.name != ".git" && it.name != ".gradle" }
            .filter { it.isFile && it.extension == "kt" && !it.path.contains("test") }
}
