package app.anima.core.modeldelivery

import android.content.Context
import app.anima.core.model.InstalledMindModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the model directory. v0.5 (ADR-017): SEVERAL models may be installed
 * side by side — the registry made models interchangeable data, so the Mind
 * screen lets the owner keep e.g. a multilingual Qwen next to the legacy
 * Gemma and switch between them. Which one speaks is [MindModelResolver]'s
 * call; this class only keeps files honest. Files land here only through
 * [commit], fed by the downloader or the SAF importer — both finish into a
 * `.part` staging file first, so a torn write can never masquerade as an
 * installed model.
 *
 * Lives in noBackupFilesDir: gigabyte weights must never ride
 * device-to-device transfer or any future backup config.
 */
@Singleton
class MindModelStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        val dir: File = File(context.noBackupFilesDir, "mind-models").apply { mkdirs() }

        private val state = MutableStateFlow(scan())

        /** Every valid model file on disk, newest first. */
        val models: StateFlow<List<InstalledMindModel>> = state.asStateFlow()

        /** Free bytes on the volume that hosts the model dir. */
        fun freeBytes(): Long = dir.usableSpace

        fun stagingFile(finalName: String): File = File(dir, "$finalName.part")

        /**
         * Promote a finished staging file to an installed model. Other
         * installed models SURVIVE (v0.5 multi-model); only stale staging
         * leftovers are swept.
         */
        fun commit(staging: File): InstalledMindModel {
            require(staging.parentFile == dir && staging.name.endsWith(".part")) {
                "commit expects a staging file inside the model dir"
            }
            val finalFile = File(dir, staging.name.removeSuffix(".part"))
            // Re-importing the same name replaces that file, never neighbors.
            finalFile.delete()
            dir.listFiles()?.forEach { existing ->
                if (existing != staging && existing.name.endsWith(".part")) existing.delete()
            }
            check(staging.renameTo(finalFile)) { "could not finalize ${finalFile.name}" }
            refresh()
            return checkNotNull(state.value.firstOrNull { it.fileName == finalFile.name }) {
                "committed model failed validation scan"
            }
        }

        /** Delete one installed model by file name (Mind screen action). */
        fun deleteModel(fileName: String) {
            val target = File(dir, File(fileName).name)
            if (target.parentFile == dir && !target.name.endsWith(".part")) target.delete()
            refresh()
        }

        fun refresh() {
            state.value = scan()
        }

        private fun scan(): List<InstalledMindModel> =
            dir
                .listFiles()
                ?.filter { it.isFile && it.extension in MODEL_EXTENSIONS && it.length() >= MIN_MODEL_BYTES }
                ?.sortedByDescending { it.lastModified() }
                ?.map { InstalledMindModel(fileName = it.name, sizeBytes = it.length(), path = it.absolutePath) }
                .orEmpty()

        companion object {
            val MODEL_EXTENSIONS = setOf("task", "litertlm")

            /**
             * Coarse sanity floor — the smallest plausible artifact (Gemma 3
             * 270M q4) is ~250 MB; anything under 64 MB is a truncated file or
             * the wrong pick. The real gate is the engine's load smoke test.
             */
            const val MIN_MODEL_BYTES = 64L * 1024 * 1024
        }
    }
