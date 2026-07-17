package app.anima

import android.app.Application
import app.anima.core.data.AnimaDatabase
import app.anima.core.data.crypto.SoulKeyHolder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

@HiltAndroidApp
class AnimaApp : Application() {
    /** Provider, not instance: DB construction stays off the main thread. */
    @Inject lateinit var database: Provider<AnimaDatabase>

    @Inject lateinit var soulKeyHolder: SoulKeyHolder

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Open the encrypted DB eagerly, then scrub the Java-side passphrase.
        // SQLCipher never zeroes it itself; see SoulKeyHolder / ADR-003.
        appScope.launch {
            database.get().openHelper.writableDatabase
            soulKeyHolder.zero()
        }
    }
}
