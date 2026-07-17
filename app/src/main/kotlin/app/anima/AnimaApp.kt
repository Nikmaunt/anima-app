package app.anima

import android.app.Application
import app.anima.core.data.crypto.SoulVaultWarmer
import app.anima.feature.widget.WidgetRefresh
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AnimaApp : Application() {
    @Inject lateinit var soulVaultWarmer: SoulVaultWarmer

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Open the encrypted DB eagerly, then scrub the Java-side passphrase.
        // SQLCipher never zeroes it itself; see SoulKeyHolder / ADR-003.
        appScope.launch { soulVaultWarmer.warmUpAndScrub() }
        // Widget charge-edge triggers (ADR-007); no-op without widgets on the
        // launcher beyond two parked constraint one-shots.
        WidgetRefresh.armTriggers(this)
    }
}
