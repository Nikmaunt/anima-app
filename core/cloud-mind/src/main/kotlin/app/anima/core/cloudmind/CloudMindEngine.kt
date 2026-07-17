package app.anima.core.cloudmind

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import app.anima.core.model.CloudMindBackend
import app.anima.core.model.CloudMindConfig
import app.anima.core.model.FactCandidate
import app.anima.core.model.FactJson
import app.anima.core.model.MindEvent
import app.anima.core.model.MindFailure
import app.anima.core.model.MindPrompt
import app.anima.core.model.MindPrompts
import app.anima.core.model.MindStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection

/**
 * ADR-011: the user-keyed cloud mind. Speaks ONLY when [status] is READY —
 * which requires the user's explicit opt-in, a stored key, and a validated
 * network. Everything that leaves the device is assembled by PromptBuilder
 * upstream (persona + body + top-N facts + dialogue window) — this class
 * adds transport, never content. The key travels as one Authorization
 * header; the byte array is zeroed after use (the header String is JVM-
 * immutable residue, same class of accepted residue as ADR-003's native
 * copy — recorded in threat-model.md). No logging in this module, enforced
 * by NetworkIsolationTest v3.
 */
@Singleton
class CloudMindEngine
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val store: CloudMindConfigStore,
        private val vault: CloudKeyVault,
    ) : CloudMindBackend {
        override val config: Flow<CloudMindConfig> = store.config

        override suspend fun currentConfig(): CloudMindConfig = store.current()

        override suspend fun status(): MindStatus =
            if (store.current().usable && online()) MindStatus.READY else MindStatus.ASLEEP

        override suspend fun requestDownload(): Boolean = false

        override fun reply(prompt: MindPrompt): Flow<MindEvent> =
            flow {
                val cfg = store.current()
                if (!cfg.usable) {
                    emit(MindEvent.Failed(MindFailure.LOST_THOUGHT))
                    return@flow
                }
                val outcome =
                    runCatching {
                        val body = SseChat.requestBody(cfg.model, prompt.system, prompt.user, stream = true)
                        request(cfg, body) { connection ->
                            val full = StringBuilder()
                            connection.inputStream.bufferedReader().useLines { lines ->
                                for (line in lines) {
                                    if (SseChat.isDone(line)) break
                                    SseChat.chunkFromLine(line)?.let { chunk ->
                                        full.append(chunk)
                                        emit(MindEvent.Chunk(chunk))
                                    }
                                }
                            }
                            full.toString()
                        }
                    }
                outcome.fold(
                    onSuccess = { result ->
                        when (result) {
                            is RequestResult.Ok -> emit(MindEvent.Done(result.value))
                            is RequestResult.HttpError ->
                                emit(
                                    MindEvent.Failed(
                                        when (result.code) {
                                            RATE_LIMITED -> MindFailure.TIRED
                                            else -> MindFailure.LOST_THOUGHT
                                        },
                                    ),
                                )
                        }
                    },
                    onFailure = { emit(MindEvent.Failed(MindFailure.LOST_THOUGHT)) },
                )
            }.flowOn(Dispatchers.IO)

        override suspend fun extractFactCandidates(
            userText: String,
            creatureText: String,
        ): List<FactCandidate> =
            withContext(Dispatchers.IO) {
                runCatching {
                    val cfg = store.current()
                    if (!cfg.usable) return@withContext emptyList()
                    val body =
                        SseChat.requestBody(
                            model = cfg.model,
                            system = "",
                            user = MindPrompts.extraction(userText, creatureText),
                            stream = false,
                            temperature = EXTRACTION_TEMPERATURE,
                        )
                    val result =
                        request(cfg, body) { it.inputStream.bufferedReader().use(BufferedReader::readText) }
                    when (result) {
                        is RequestResult.Ok -> FactJson.parseCandidates(SseChat.contentFromResponse(result.value))
                        is RequestResult.HttpError -> emptyList()
                    }
                }.getOrDefault(emptyList())
            }

        /**
         * One authenticated POST. The reader lambda sees a connected stream;
         * HTTP errors short-circuit to [RequestResult.HttpError] without
         * reading the error body (it could echo the request).
         */
        private inline fun <T> request(
            cfg: CloudMindConfig,
            body: String,
            reader: (HttpURLConnection) -> T,
        ): RequestResult<T> {
            val url = URL(SseChat.completionsUrl(cfg.baseUrl))
            require(url.protocol == "https") { "cloud mind endpoints must be https" }
            val connection = url.openConnection() as HttpsURLConnection
            val key = vault.read() ?: return RequestResult.HttpError(NO_KEY)
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer " + String(key, Charsets.UTF_8))
                connection.outputStream.use { it.write(body.toByteArray()) }
                val code = connection.responseCode
                if (code !in HTTP_OK_RANGE) return RequestResult.HttpError(code)
                return RequestResult.Ok(reader(connection))
            } finally {
                key.fill(0)
                connection.disconnect()
            }
        }

        private sealed interface RequestResult<out T> {
            data class Ok<T>(
                val value: T,
            ) : RequestResult<T>

            data class HttpError(
                val code: Int,
            ) : RequestResult<Nothing>
        }

        private fun online(): Boolean {
            val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
            val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }

        private companion object {
            const val CONNECT_TIMEOUT_MS = 10_000
            const val READ_TIMEOUT_MS = 30_000
            const val RATE_LIMITED = 429
            const val NO_KEY = -1
            val HTTP_OK_RANGE = 200..299
            const val EXTRACTION_TEMPERATURE = 0.2
        }
    }
