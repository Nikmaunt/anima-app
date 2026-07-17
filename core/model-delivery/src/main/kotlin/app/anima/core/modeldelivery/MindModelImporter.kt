package app.anima.core.modeldelivery

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "Bring the file yourself" — the zero-network delivery path and the primary
 * one in practice: official Gemma artifacts are license-gated, so the user
 * downloads in a browser (accepting Google's license there) and picks the
 * file via SAF. We stream-copy into staging, then reuse the downloader's
 * size/hash/commit tail.
 */
@Singleton
class MindModelImporter
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val store: MindModelStore,
        private val downloader: MindModelDownloader,
    ) {
        fun import(
            uri: Uri,
            expectedSha256: String? = null,
        ): Flow<DeliveryEvent> =
            flow {
                val (name, size) = describe(uri)
                if (name == null || name.substringAfterLast('.') !in MindModelStore.MODEL_EXTENSIONS) {
                    emit(DeliveryEvent.Failed(DeliveryFailure.NOT_A_MODEL, name ?: uri.toString()))
                    return@flow
                }
                if (size != null && store.freeBytes() < size + SPACE_MARGIN_BYTES) {
                    emit(DeliveryEvent.Failed(DeliveryFailure.NO_SPACE))
                    return@flow
                }
                val staging = store.stagingFile(name)
                staging.delete()
                try {
                    val input = context.contentResolver.openInputStream(uri)
                    if (input == null) {
                        emit(DeliveryEvent.Failed(DeliveryFailure.INTERRUPTED, "content stream unavailable"))
                        return@flow
                    }
                    var done = 0L
                    var sinceEmit = 0L
                    input.use { source ->
                        staging.outputStream().use { out ->
                            val buffer = ByteArray(BUFFER_BYTES)
                            while (true) {
                                val read = source.read(buffer)
                                if (read < 0) break
                                out.write(buffer, 0, read)
                                done += read
                                sinceEmit += read
                                if (sinceEmit >= EMIT_EVERY_BYTES) {
                                    sinceEmit = 0
                                    emit(DeliveryEvent.Progress(done, size))
                                }
                            }
                        }
                    }
                    emit(DeliveryEvent.Progress(done, size))
                    emit(downloader.finalize(staging, expectedSha256))
                } catch (e: IOException) {
                    staging.delete()
                    emit(DeliveryEvent.Failed(DeliveryFailure.INTERRUPTED, e.message))
                }
            }.flowOn(Dispatchers.IO)

        private fun describe(uri: Uri): Pair<String?, Long?> =
            context.contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    if (!cursor.moveToFirst()) return@use null to null
                    val name = cursor.getString(0)
                    val size = if (cursor.isNull(1)) null else cursor.getLong(1)
                    name to size
                } ?: (uri.lastPathSegment to null)

        private companion object {
            const val BUFFER_BYTES = 256 * 1024
            const val EMIT_EVERY_BYTES = 8L * 1024 * 1024
            const val SPACE_MARGIN_BYTES = 128L * 1024 * 1024
        }
    }
