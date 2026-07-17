package app.anima.core.data.prefs

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notification-sense config, out-of-band in SharedPreferences (hermes-lens
 * `notif:config` pattern): the listener service reads it synchronously on
 * every notification with no coroutine round-trip, and settings write-through
 * on every change so the listener can never see a stale copy.
 */
@Singleton
class NotifConfigStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val prefs: SharedPreferences =
            context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

        data class Config(
            val enabled: Boolean,
            val storeText: Boolean,
            val allowlist: Set<String>,
            val quietHoursEnabled: Boolean = false,
            val quietFromMinute: Int = DEFAULT_QUIET_FROM,
            val quietUntilMinute: Int = DEFAULT_QUIET_UNTIL,
        ) {
            /** Quiet hours v2: the creature "doesn't hear" at night. */
            fun isQuietAt(minuteOfDay: Int): Boolean {
                if (!quietHoursEnabled) return false
                return if (quietFromMinute <= quietUntilMinute) {
                    minuteOfDay in quietFromMinute until quietUntilMinute
                } else {
                    // Wraps midnight (23:00 → 07:00).
                    minuteOfDay >= quietFromMinute || minuteOfDay < quietUntilMinute
                }
            }
        }

        fun read(): Config =
            Config(
                enabled = prefs.getBoolean(KEY_ENABLED, false),
                storeText = prefs.getBoolean(KEY_STORE_TEXT, false),
                allowlist = prefs.getStringSet(KEY_ALLOWLIST, emptySet())?.toSet() ?: emptySet(),
                quietHoursEnabled = prefs.getBoolean(KEY_QUIET_ENABLED, false),
                quietFromMinute = prefs.getInt(KEY_QUIET_FROM, DEFAULT_QUIET_FROM),
                quietUntilMinute = prefs.getInt(KEY_QUIET_UNTIL, DEFAULT_QUIET_UNTIL),
            )

        fun write(config: Config) {
            prefs
                .edit()
                .putBoolean(KEY_ENABLED, config.enabled)
                .putBoolean(KEY_STORE_TEXT, config.storeText)
                .putStringSet(KEY_ALLOWLIST, config.allowlist)
                .putBoolean(KEY_QUIET_ENABLED, config.quietHoursEnabled)
                .putInt(KEY_QUIET_FROM, config.quietFromMinute)
                .putInt(KEY_QUIET_UNTIL, config.quietUntilMinute)
                .apply()
        }

        companion object {
            /** 23:00 → 07:00 local. */
            const val DEFAULT_QUIET_FROM = 23 * 60
            const val DEFAULT_QUIET_UNTIL = 7 * 60
            private const val FILE = "notif_config"
            private const val KEY_ENABLED = "enabled"
            private const val KEY_STORE_TEXT = "store_text"
            private const val KEY_ALLOWLIST = "allowlist"
            private const val KEY_QUIET_ENABLED = "quiet_enabled"
            private const val KEY_QUIET_FROM = "quiet_from"
            private const val KEY_QUIET_UNTIL = "quiet_until"
        }
    }
