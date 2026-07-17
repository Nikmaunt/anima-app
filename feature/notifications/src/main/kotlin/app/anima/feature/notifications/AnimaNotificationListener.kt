package app.anima.feature.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import app.anima.core.data.prefs.NotifConfigStore
import app.anima.core.data.repo.NotifEventsRepository
import app.anima.core.model.NotifCaptureFilter
import app.anima.core.model.NotifIdentity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Anima's ONLY background entity (constraint #5). It does exactly one thing:
 * filter → write to the local encrypted DB. Zero inference, zero network
 * (this module has no network-capable dependency and the app has no INTERNET
 * permission), zero UI, zero wake locks.
 *
 * hermes-lens listener discipline carried over:
 * - the whole handler is wrapped in swallow-everything — a listener exception
 *   takes down the process, and notification content must never hit the log;
 * - every gate runs BEFORE anything is persisted (enabled → allowlist →
 *   group summary → ongoing → OTP), and an OTP hit drops the notification
 *   whole, even from an allowlisted app;
 * - config is read synchronously from SharedPreferences on every event, so a
 *   settings change applies instantly with no stale copy.
 */
@AndroidEntryPoint
class AnimaNotificationListener : NotificationListenerService() {
    @Inject lateinit var configStore: NotifConfigStore

    @Inject lateinit var events: NotifEventsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        try {
            capture(sbn ?: return)
        } catch (ignored: Throwable) {
            // Never rethrow, never log content: one dropped event, not a crash.
        }
    }

    private fun capture(sbn: StatusBarNotification) {
        val config = configStore.read()
        // Quiet hours (v2): at night the creature doesn't hear — the event is
        // dropped BEFORE any field is read, not stored-and-hidden.
        val calendar = java.util.Calendar.getInstance()
        val minuteOfDay =
            calendar.get(java.util.Calendar.HOUR_OF_DAY) * 60 + calendar.get(java.util.Calendar.MINUTE)
        if (config.isQuietAt(minuteOfDay)) return
        val extras = sbn.notification?.extras
        val captured =
            NotifCaptureFilter.gate(
                enabled = config.enabled,
                allowlist = config.allowlist,
                packageName = sbn.packageName,
                isGroupSummary = (sbn.notification?.flags ?: 0) and Notification.FLAG_GROUP_SUMMARY != 0,
                isOngoing = sbn.isOngoing,
                title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
                text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
                storeText = config.storeText,
            ) ?: return

        val id = NotifIdentity.entryId(captured.packageName, sbn.postTime, captured.title, captured.text)
        scope.launch {
            try {
                events.insert(id, captured.packageName, sbn.postTime, captured.title, captured.text)
            } catch (ignored: Throwable) {
                // Same rule: a failed write drops one event silently.
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
