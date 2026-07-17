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
class NotifConfigStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    data class Config(
        val enabled: Boolean,
        val storeText: Boolean,
        val allowlist: Set<String>,
    )

    fun read(): Config = Config(
        enabled = prefs.getBoolean(KEY_ENABLED, false),
        storeText = prefs.getBoolean(KEY_STORE_TEXT, false),
        allowlist = prefs.getStringSet(KEY_ALLOWLIST, emptySet())?.toSet() ?: emptySet(),
    )

    fun write(config: Config) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, config.enabled)
            .putBoolean(KEY_STORE_TEXT, config.storeText)
            .putStringSet(KEY_ALLOWLIST, config.allowlist)
            .apply()
    }

    private companion object {
        const val FILE = "notif_config"
        const val KEY_ENABLED = "enabled"
        const val KEY_STORE_TEXT = "store_text"
        const val KEY_ALLOWLIST = "allowlist"
    }
}
