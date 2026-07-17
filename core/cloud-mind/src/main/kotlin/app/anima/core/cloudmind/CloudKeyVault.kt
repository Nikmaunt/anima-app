package app.anima.core.cloudmind

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrap/unwrap for the BYOK API key (ADR-011). Same construction as the soul
 * passphrase wrap (ADR-003) under a SEPARATE Keystore alias: a compromise of
 * one secret's ceremony must not be a skeleton key for the other. Split into
 * an interface so Robolectric tests can exercise the vault file handling
 * without AndroidKeyStore.
 */
interface SecretCipher {
    fun wrap(plain: ByteArray): ByteArray

    fun unwrap(blob: ByteArray): ByteArray
}

class KeystoreSecretCipher
    @Inject
    constructor() : SecretCipher {
        private fun keystoreKey(): SecretKey {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            (ks.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            generator.init(
                KeyGenParameterSpec
                    .Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
            return generator.generateKey()
        }

        override fun wrap(plain: ByteArray): ByteArray {
            val cipher = Cipher.getInstance(TRANSFORM)
            cipher.init(Cipher.ENCRYPT_MODE, keystoreKey())
            return cipher.iv + cipher.doFinal(plain)
        }

        override fun unwrap(blob: ByteArray): ByteArray {
            val cipher = Cipher.getInstance(TRANSFORM)
            val spec = GCMParameterSpec(GCM_TAG_BITS, blob, 0, IV_LENGTH)
            cipher.init(Cipher.DECRYPT_MODE, keystoreKey(), spec)
            return cipher.doFinal(blob, IV_LENGTH, blob.size - IV_LENGTH)
        }

        private companion object {
            const val ANDROID_KEYSTORE = "AndroidKeyStore"
            const val KEY_ALIAS = "anima_cloud_key_wrap"
            const val TRANSFORM = "AES/GCM/NoPadding"
            const val IV_LENGTH = 12
            const val GCM_TAG_BITS = 128
        }
    }

/**
 * Custody of the wrapped API key on disk: `cloud.key` in noBackupFilesDir —
 * never rides backups or device transfer, never appears in the soul export
 * (the export reads the DB, not this directory; NetworkIsolationTest v3
 * additionally greps the export codec for this filename). The plaintext key
 * exists in memory only for the duration of one request-header build; the
 * returned array is the caller's to zero. A corrupt file fails loudly on
 * read (GCM tag) — the UI answer is "re-enter the key", never silent reuse.
 */
@Singleton
class CloudKeyVault
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val cipher: SecretCipher,
    ) {
        private val file: File get() = File(context.noBackupFilesDir, KEY_FILE)

        fun hasKey(): Boolean = file.exists() && file.length() > 0

        fun store(key: ByteArray) {
            val atomic = AtomicFile(file)
            val out = atomic.startWrite()
            try {
                out.write(cipher.wrap(key))
                atomic.finishWrite(out)
            } catch (t: Throwable) {
                atomic.failWrite(out)
                throw t
            } finally {
                key.fill(0)
            }
        }

        /** Plaintext key; caller must zero it right after use. Null = no key. */
        fun read(): ByteArray? {
            if (!hasKey()) return null
            return cipher.unwrap(AtomicFile(file).readFully())
        }

        fun clear() {
            file.delete()
        }

        private companion object {
            const val KEY_FILE = "cloud.key"
        }
    }
