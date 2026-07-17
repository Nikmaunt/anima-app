package app.anima.feature.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters

/**
 * ADR-007 trigger reality: none of ACTION_BATTERY_LOW/OKAY/POWER_CONNECTED/
 * DISCONNECTED are manifest-receivable on API 26+ (research-v2 §B.1), so the
 * plan's broadcast triggers are replaced by what the platform actually
 * offers a dead process:
 *
 * - the 30-minute heartbeat: `updatePeriodMillis` in the provider XML
 *   (system-driven, no WorkManager periodic needed);
 * - charge-started edge: a one-shot with `requiresCharging` that re-arms
 *   itself after each firing;
 * - battery-recovered edge: a one-shot with `requiresBatteryNotLow`, armed
 *   opportunistically alongside.
 *
 * Battery-LOW has no negative constraint; that edge waits for the heartbeat
 * — recorded as the honest gap in ADR-007.
 */
object WidgetRefresh {
    private const val CHARGE_WORK = "widget-charge-edge"
    private const val RECOVERED_WORK = "widget-battery-recovered"

    /** Idempotent; call at app start and after each worker firing. */
    fun armTriggers(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.enqueueUniqueWork(
            CHARGE_WORK,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
                .setConstraints(Constraints.Builder().setRequiresCharging(true).build())
                .build(),
        )
        wm.enqueueUniqueWork(
            RECOVERED_WORK,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build(),
        )
    }
}

/** Renders the fresh snapshot into every widget instance, then re-arms. */
class WidgetRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        runCatching { AnimaWidget().updateAll(applicationContext) }
        // Re-arm AFTER updating: the constraint has just been satisfied, so
        // the next enqueue waits for the NEXT edge, not this one.
        WidgetRefresh.armTriggers(applicationContext)
        return Result.success()
    }
}
