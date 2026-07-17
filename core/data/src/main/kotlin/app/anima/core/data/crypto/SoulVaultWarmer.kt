package app.anima.core.data.crypto

import app.anima.core.data.AnimaDatabase
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Startup ritual (ADR-003 addendum): force the encrypted DB open once, then
 * scrub the Java-side passphrase. Lives in core:data so :app never needs
 * Room on its compile classpath.
 */
@Singleton
class SoulVaultWarmer
    @Inject
    constructor(
        private val database: Provider<AnimaDatabase>,
        private val keyHolder: SoulKeyHolder,
    ) {
        fun warmUpAndScrub() {
            database.get().openHelper.writableDatabase
            keyHolder.zero()
        }
    }
