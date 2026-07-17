package app.anima.core.modeldelivery

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.IOException
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection

/**
 * The one place in the app that opens a socket (ADR-005). Explicit user
 * action only; runs while the Mind screen is open (no service — the
 * background-entity budget stays "listener + widget triggers"). Resumable
 * via HTTP Range against the staging file; SHA-256 verified when the user
 * supplies an expected hash (official Gemma hashes are license-gated, so
 * pinning is the owner's step — research-v2 §A.4).
 */
@Singleton
class MindModelDownloader
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val store: MindModelStore,
    ) {
        fun download(
            url: String,
            expectedSha256: String?,
            allowMetered: Boolean,
            expectedBytes: Long? = null,
        ): Flow<DeliveryEvent> =
            flow {
                val parsed = runCatching { URL(url) }.getOrNull()
                if (parsed == null || parsed.protocol != "https") {
                    emit(DeliveryEvent.Failed(DeliveryFailure.NOT_HTTPS))
                    return@flow
                }
                val fileName = parsed.path.substringAfterLast('/').ifBlank { DEFAULT_NAME }
                if (fileName.substringAfterLast('.') !in MindModelStore.MODEL_EXTENSIONS) {
                    emit(DeliveryEvent.Failed(DeliveryFailure.NOT_A_MODEL, fileName))
                    return@flow
                }
                if (!allowMetered && !onUnmeteredNetwork()) {
                    emit(DeliveryEvent.Failed(DeliveryFailure.NEEDS_WIFI))
                    return@flow
                }
                if (expectedBytes != null && store.freeBytes() < expectedBytes + SPACE_MARGIN_BYTES) {
                    emit(DeliveryEvent.Failed(DeliveryFailure.NO_SPACE))
                    return@flow
                }

                val staging = store.stagingFile(fileName)
                val resumeFrom = staging.length()
                val connection =
                    (parsed.openConnection() as HttpsURLConnection).apply {
                        connectTimeout = CONNECT_TIMEOUT_MS
                        readTimeout = READ_TIMEOUT_MS
                        if (resumeFrom > 0) setRequestProperty("Range", "bytes=$resumeFrom-")
                    }
                try {
                    val code = connection.responseCode
                    val resuming = code == HttpURLConnection.HTTP_PARTIAL && resumeFrom > 0
                    if (code != HttpURLConnection.HTTP_OK && !resuming) {
                        emit(DeliveryEvent.Failed(DeliveryFailure.HTTP_ERROR, "HTTP $code"))
                        return@flow
                    }
                    if (!resuming) staging.delete()
                    val offset = if (resuming) resumeFrom else 0L
                    val total = connection.contentLengthLong.takeIf { it > 0 }?.plus(offset)

                    var done = offset
                    connection.inputStream.use { input ->
                        RandomAccessFile(staging, "rw").use { out ->
                            out.seek(offset)
                            val buffer = ByteArray(BUFFER_BYTES)
                            var sinceEmit = 0L
                            while (true) {
                                val read = input.read(buffer)
                                if (read < 0) break
                                out.write(buffer, 0, read)
                                done += read
                                sinceEmit += read
                                if (sinceEmit >= EMIT_EVERY_BYTES) {
                                    sinceEmit = 0
                                    emit(DeliveryEvent.Progress(done, total))
                                }
                            }
                        }
                    }
                    emit(DeliveryEvent.Progress(done, total))
                    emit(finalize(staging, expectedSha256))
                } catch (e: IOException) {
                    // Staging file survives — the next attempt resumes.
                    emit(DeliveryEvent.Failed(DeliveryFailure.INTERRUPTED, e.message))
                } finally {
                    connection.disconnect()
                }
            }.flowOn(Dispatchers.IO)

        /** Shared tail: size gate → optional hash gate → commit. */
        internal fun finalize(
            staging: java.io.File,
            expectedSha256: String?,
        ): DeliveryEvent {
            if (staging.length() < MindModelStore.MIN_MODEL_BYTES) {
                staging.delete()
                return DeliveryEvent.Failed(DeliveryFailure.NOT_A_MODEL, "file too small")
            }
            if (!expectedSha256.isNullOrBlank()) {
                val actual = sha256(staging)
                if (!actual.equals(expectedSha256.trim(), ignoreCase = true)) {
                    staging.delete()
                    return DeliveryEvent.Failed(DeliveryFailure.CHECKSUM_MISMATCH, actual)
                }
            }
            return DeliveryEvent.Done(store.commit(staging))
        }

        private fun onUnmeteredNetwork(): Boolean {
            val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
            val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        }

        private fun sha256(file: java.io.File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(BUFFER_BYTES)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }

        private companion object {
            const val DEFAULT_NAME = "mind-model.task"
            const val BUFFER_BYTES = 256 * 1024
            const val EMIT_EVERY_BYTES = 4L * 1024 * 1024
            const val SPACE_MARGIN_BYTES = 128L * 1024 * 1024
            const val CONNECT_TIMEOUT_MS = 15_000
            const val READ_TIMEOUT_MS = 30_000
        }
    }
