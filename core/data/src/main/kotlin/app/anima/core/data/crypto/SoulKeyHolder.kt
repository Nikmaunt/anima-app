package app.anima.core.data.crypto

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Custody of the plaintext passphrase for the lifetime of the process.
 *
 * Why it must stay alive (ADR-003 addendum v0.4): sqlcipher-android retains
 * the exact array reference we hand the factory and RE-READS it every time
 * the connection pool opens a new physical connection —
 * `SQLiteConnection.open()` → `nativeKey(configuration.password)`, verified
 * against 4.17.0 bytecode (audit-v03 §1; same aliasing as 4.6.1). In WAL
 * mode Room's pool opens non-primary connections at any moment under
 * concurrent load, so zeroing this array after the first open (the
 * v0.2/v0.3 ritual) turned routine pool growth into a fatal
 * SQLiteNotADatabaseException — reproduced live in the v0.4 emulator pass
 * and pinned by WalPoolKeyDeviceTest. The plaintext window is therefore the
 * process lifetime; at rest the passphrase only ever exists
 * Keystore-wrapped (KeystoreSoulKeySource).
 */
@Singleton
class SoulKeyHolder
    @Inject
    constructor(
        private val source: SoulKeySource,
    ) {
        @Volatile
        private var bytes: ByteArray? = null

        @Synchronized
        fun passphrase(): ByteArray = bytes ?: source.passphrase().also { bytes = it }
    }
