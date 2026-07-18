package app.anima

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.StrictMode
import app.anima.core.data.crypto.SoulVaultWarmer
import app.anima.core.modeldelivery.PackModelSource
import app.anima.feature.settings.CrashLog
import app.anima.feature.widget.WidgetRefresh
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltAndroidApp
class AnimaApp : Application() {
    @Inject lateinit var soulVaultWarmer: SoulVaultWarmer

    @Inject lateinit var packModelSource: PackModelSource

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Open the encrypted DB eagerly so a corrupt soul.key fails at
        // launch. The passphrase is deliberately NOT scrubbed: the WAL pool
        // re-keys every new connection from it — SoulKeyHolder / ADR-003
        // addendum v0.4.
        appScope.launch { soulVaultWarmer.warmUp() }
        // Plaintext soul exports are share-sheet ephemera (audit-v03 F2):
        // whatever a previous session left in cache/exports dies here.
        appScope.launch {
            java.io
                .File(cacheDir, "exports")
                .listFiles()
                ?.forEach { it.delete() }
        }
        // ADR-010: learn whether Play has (or is fetching) the mind pack.
        // Observation only — the fast-follow download is Play's own doing.
        packModelSource.refresh()
        // Widget charge-edge triggers (ADR-007); no-op without widgets on the
        // launcher beyond two parked constraint one-shots.
        WidgetRefresh.armTriggers(this)
        // Local crash log (threat-model: self-diagnosis without telemetry).
        installCrashRecorder()
        // StrictMode in debug builds only (Phase 3 security list): thread and
        // VM policies log violations; nothing in release.
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy
                    .Builder()
                    .detectAll()
                    .penaltyLog()
                    .build(),
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy
                    .Builder()
                    .detectLeakedClosableObjects()
                    .detectActivityLeaks()
                    .penaltyLog()
                    .build(),
            )
        }
    }

    /** Chain, never replace: write the file, then let the system die loudly. */
    private fun installCrashRecorder() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val file = CrashLog.file(this)
                file.parentFile?.mkdirs()
                file.writeText(
                    "Anima crash at ${Instant.now()}\nthread: ${thread.name}\n\n" +
                        throwable.stackTraceToString(),
                )
            }
            previous?.uncaughtException(thread, throwable)
        }
    }
}
