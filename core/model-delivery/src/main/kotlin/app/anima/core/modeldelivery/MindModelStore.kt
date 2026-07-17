package app.anima.core.modeldelivery

import android.content.Context
import app.anima.core.model.InstalledMindModel
import app.anima.core.model.MindModelLocator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the model directory. One model at a time (the S24 doesn't have space
 * ambitions); files land here only through [commit], which is fed by the
 * downloader or the SAF importer — both finish into a `.part` staging file
 * first, so a torn write can never masquerade as an installed model.
 *
 * Lives in noBackupFilesDir: a 500 MB model must never ride device-to-device
 * transfer or any future backup config.
 */
@Singleton
class MindModelStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : MindModelLocator {
        val dir: File = File(context.noBackupFilesDir, "mind-models").apply { mkdirs() }

        private val state = MutableStateFlow(scan())
        override val installed: StateFlow<InstalledMindModel?> = state.asStateFlow()

        /** Free bytes on the volume that hosts the model dir. */
        fun freeBytes(): Long = dir.usableSpace

        fun stagingFile(finalName: String): File = File(dir, "$finalName.part")

        /**
         * Promote a finished staging file to the installed model. Any previous
         * model (and stray staging leftovers for other names) is deleted —
         * one mind per phone.
         */
        fun commit(staging: File): InstalledMindModel {
            require(staging.parentFile == dir && staging.name.endsWith(".part")) {
                "commit expects a staging file inside the model dir"
            }
            val finalFile = File(dir, staging.name.removeSuffix(".part"))
            dir.listFiles()?.forEach { existing ->
                if (existing != staging) existing.delete()
            }
            check(staging.renameTo(finalFile)) { "could not finalize ${finalFile.name}" }
            refresh()
            return checkNotNull(state.value) { "committed model failed validation scan" }
        }

        /** Delete the installed model (Mind screen action). Staging survives. */
        fun deleteInstalled() {
            dir.listFiles()?.forEach { if (!it.name.endsWith(".part")) it.delete() }
            refresh()
        }

        fun refresh() {
            state.value = scan()
        }

        private fun scan(): InstalledMindModel? =
            dir
                .listFiles()
                ?.filter { it.isFile && it.extension in MODEL_EXTENSIONS && it.length() >= MIN_MODEL_BYTES }
                ?.maxByOrNull { it.lastModified() }
                ?.let { InstalledMindModel(fileName = it.name, sizeBytes = it.length(), path = it.absolutePath) }

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
