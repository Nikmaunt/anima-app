package app.anima.core.data.crypto

import app.anima.core.data.AnimaDatabase
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Startup ritual (ADR-003 addendum v0.4): force the encrypted DB open once
 * so a corrupt/tampered `soul.key` fails loudly at launch, not mid-session.
 * The passphrase is NOT scrubbed afterwards — the WAL pool re-keys every
 * new physical connection from the retained array (see SoulKeyHolder), so
 * the key must outlive the pool. Lives in core:data so :app never needs
 * Room on its compile classpath.
 */
@Singleton
class SoulVaultWarmer
    @Inject
    constructor(
        private val database: Provider<AnimaDatabase>,
    ) {
        fun warmUp() {
            database.get().openHelper.writableDatabase
        }
    }
