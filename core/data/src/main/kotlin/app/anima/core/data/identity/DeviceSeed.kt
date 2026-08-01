package app.anima.core.data.identity

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import app.anima.core.data.prefs.AnimaPrefs
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The number this phone's creature is made of.
 *
 * Moved out of `OnboardingViewModel` in v1.1c, because it is no longer only
 * onboarding's business: the identity repair migration needs it too, and a
 * second copy of this function would be a second creature.
 *
 * `ANDROID_ID` is per-app-signing scoped, needs no permission, and survives
 * everything except a factory reset — which is why it is the anchor for
 * "this body belongs to this phone".
 *
 * ### What happens when it cannot be read
 *
 * It used to degrade to the FNV-1a of the literal string `"anima-fallback"`.
 * That is the default-body class again, in its worst form: every phone that hit
 * the failure got not just the same body but the same *life* — same hue, same
 * size, same blink rate, same curiosity, same name. A wrong body is one frame;
 * a shared seed is a shared identity.
 *
 * Now: roll a random seed **once**, write it down, and use the written one
 * forever after. The body stays unique to the device and stays fixed for it,
 * which is the whole promise. The rolled seed lives in the same DataStore as
 * the rest of the non-secret preferences, so it survives app restarts and dies
 * with the app's data, exactly like `ANDROID_ID` would.
 */
@Singleton
class DeviceSeed
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val prefs: AnimaPrefs,
    ) {
        /**
         * Suspends because the fallback path has to read (and possibly write)
         * durable storage. The happy path — a readable `ANDROID_ID` — never
         * touches it.
         */
        suspend fun value(): Long {
            readAndroidId()?.let { return fnv1a(it) }
            prefs.deviceSeedFallback()?.let { return it }
            return prefs.rememberDeviceSeedFallback(SecureRandom().nextLong())
        }

        /**
         * True when this phone is running on a rolled seed rather than on its
         * own device id. Surfaced in the creature passport, because "this body
         * belongs to this phone" is a slightly weaker claim in that case: a
         * reinstall re-rolls, where a readable device id would not.
         */
        suspend fun isRolled(): Boolean = readAndroidId() == null

        @SuppressLint("HardwareIds")
        private fun readAndroidId(): String? =
            runCatching {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            }.getOrNull()
                ?.takeIf { it.isNotBlank() }

        private fun fnv1a(text: String): Long {
            var hash = FNV_OFFSET_BASIS
            for (ch in text) {
                hash = hash xor ch.code.toLong()
                hash *= FNV_PRIME
            }
            return hash
        }

        private companion object {
            const val FNV_OFFSET_BASIS = -0x340d631b7bdddcdbL
            const val FNV_PRIME = 0x100000001b3L
        }
    }
