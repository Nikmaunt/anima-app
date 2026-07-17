package app.anima.core.data.crypto

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Custody of the plaintext passphrase between DB construction and the eager
 * first open. SQLCipher's Support layer retains the exact array reference we
 * hand it and never scrubs it (verified against sqlcipher-android 4.6.1
 * bytecode), so zeroing *this* array also scrubs the copy the factory holds.
 *
 * Contract: [zero] may only be called after the database has actually been
 * opened once (AnimaApp does this eagerly at startup). After zeroing, a
 * re-open within the same process would fail loudly — accepted, because the
 * Room singleton keeps its connection for the process lifetime and a fresh
 * process re-derives the passphrase from the Keystore-wrapped file.
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

        /** Scrub the Java-side copy. Native key material remains — ADR-003. */
        @Synchronized
        fun zero() {
            bytes?.fill(0)
            bytes = null
        }
    }
