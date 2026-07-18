package app.anima.core.voice

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** Result of the offline-voice check that gates every spoken word (ADR-013). */
sealed interface VoiceAvailability {
    /** An offline voice is selected; [voiceName] is shown in Settings. */
    data class Ready(
        val voiceName: String,
    ) : VoiceAvailability

    /** Engine alive, but no offline voice for the language: the creature stays mute. */
    data object NoOfflineVoice : VoiceAvailability

    /** No TTS engine, or it failed to init. Reported honestly, never retried in a loop. */
    data object EngineUnavailable : VoiceAvailability
}

/**
 * The creature's mouth over the system TTS engine (ADR-013).
 *
 * Constraints that are the point of this class:
 * - The engine binds lazily on the first [checkAvailability] after the owner
 *   enables the voice; [speak] never initializes anything by itself.
 * - Text may only reach a voice that [checkAvailability] selected — offline
 *   voices exclusively, so utterances never leave the device.
 * - Speech is finite episodes: stopping on screen-off or navigation is the
 *   CALLER's duty (call [stop] from the lifecycle owner); this class never
 *   speaks in the background on its own.
 */
@Singleton
class CreatureVoice
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val initMutex = Mutex()
        private val utteranceSeq = AtomicLong()
        private val speakingState = MutableStateFlow(false)

        @Volatile
        private var engine: TextToSpeech? = null

        @Volatile
        private var chosenVoice: Voice? = null

        /** True between onStart and onDone/onError/onStop — the mouth/glow envelope. */
        val speaking: StateFlow<Boolean> = speakingState.asStateFlow()

        // UtteranceProgressListener callbacks arrive on the engine's binder
        // thread; StateFlow.value writes are thread-safe, so Main collectors
        // see a consistent envelope without any handler hop.
        private val progress =
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    speakingState.value = true
                }

                override fun onDone(utteranceId: String?) {
                    speakingState.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    speakingState.value = false
                }

                override fun onError(
                    utteranceId: String?,
                    errorCode: Int,
                ) {
                    speakingState.value = false
                }

                override fun onStop(
                    utteranceId: String?,
                    interrupted: Boolean,
                ) {
                    speakingState.value = false
                }
            }

        /**
         * Runs on first enable and whenever Settings re-checks. Selection rule
         * (ADR-013): offline voices only (`isNetworkConnectionRequired == false`),
         * language match is enough, ranked by quality descending — the engine
         * default voice is never trusted blindly. A Ready result arms [speak];
         * anything else disarms it.
         */
        suspend fun checkAvailability(locale: Locale): VoiceAvailability {
            val tts = obtainEngine() ?: return VoiceAvailability.EngineUnavailable
            val best =
                runCatching { tts.voices }
                    .getOrNull()
                    .orEmpty()
                    .filterNotNull()
                    .filter { !it.isNetworkConnectionRequired && it.locale.language == locale.language }
                    .sortedWith(
                        compareByDescending<Voice> { it.quality }
                            .thenByDescending { it.locale == locale }
                            .thenBy { it.name },
                    ).firstOrNull()
            chosenVoice = best
            return if (best == null) VoiceAvailability.NoOfflineVoice else VoiceAvailability.Ready(best.name)
        }

        /**
         * One finite spoken episode, flushing anything previous. Silently a
         * no-op unless [checkAvailability] selected an offline voice — text
         * must never fall through to a network voice or the engine default.
         */
        fun speak(
            text: String,
            character: VoiceCharacter,
        ) {
            val tts = engine ?: return
            val voice = chosenVoice ?: return
            if (text.isBlank()) return
            tts.voice = voice
            tts.setPitch(character.pitch)
            tts.setSpeechRate(character.rate)
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "anima-voice-${utteranceSeq.incrementAndGet()}")
        }

        /** Cuts speech immediately (screen-off, navigation, toggle flipped off). */
        fun stop() {
            engine?.stop()
            speakingState.value = false
        }

        /** Releases the engine; the next [checkAvailability] re-initializes. */
        fun shutdown() {
            engine?.shutdown()
            engine = null
            chosenVoice = null
            speakingState.value = false
        }

        /**
         * Deep link for the no-offline-voice state in Settings: the user
         * installs voice packs there — the app cannot download them (ADR-013).
         */
        fun ttsSettingsIntent(): Intent = Intent(ACTION_TTS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        private suspend fun obtainEngine(): TextToSpeech? {
            engine?.let { return it }
            return initMutex.withLock {
                engine ?: initEngine()?.also {
                    it.setOnUtteranceProgressListener(progress)
                    engine = it
                }
            }
        }

        /** One honest attempt per call: a failed engine is shut down, not retried. */
        private suspend fun initEngine(): TextToSpeech? =
            suspendCancellableCoroutine { cont ->
                var tts: TextToSpeech? = null
                tts =
                    TextToSpeech(context) { status ->
                        if (!cont.isActive) return@TextToSpeech
                        if (status == TextToSpeech.SUCCESS) {
                            cont.resume(tts)
                        } else {
                            tts?.shutdown()
                            cont.resume(null)
                        }
                    }
                cont.invokeOnCancellation { tts?.shutdown() }
            }

        private companion object {
            /** Public settings action; no SDK constant exists for this screen. */
            const val ACTION_TTS_SETTINGS = "com.android.settings.TTS_SETTINGS"
        }
    }
