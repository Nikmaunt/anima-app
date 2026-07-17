package app.anima.core.data.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/** Where the database passphrase comes from. Fakes replace this in tests. */
interface SoulKeySource {
    /** 32-byte passphrase; caller must not retain it longer than DB open. */
    fun passphrase(): ByteArray
}

/**
 * ADR-003: a random 32-byte passphrase, generated once, wrapped by a
 * non-exportable AES-GCM key in Android Keystore, stored (wrapped) in
 * noBackupFilesDir. The Keystore key never leaves hardware; losing it
 * (factory reset) loses the soul — the product answer is soul export.
 */
@Singleton
class KeystoreSoulKeySource @Inject constructor(
    @ApplicationContext private val context: Context,
) : SoulKeySource {

    override fun passphrase(): ByteArray {
        val file = File(context.noBackupFilesDir, WRAPPED_FILE)
        val atomic = AtomicFile(file)
        if (file.exists()) {
            val blob = atomic.readFully()
            if (blob.size > IV_LENGTH) return unwrap(blob)
        }
        val fresh = ByteArray(PASSPHRASE_BYTES).also { SecureRandom().nextBytes(it) }
        val wrapped = wrap(fresh)
        val out = atomic.startWrite()
        try {
            out.write(wrapped)
            atomic.finishWrite(out)
        } catch (t: Throwable) {
            atomic.failWrite(out)
            throw t
        }
        return fresh
    }

    private fun keystoreKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (ks.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                // No user-auth requirement: the creature must wake without
                // biometrics; at-rest protection is the threat model here.
                .build(),
        )
        return generator.generateKey()
    }

    private fun wrap(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey())
        val iv = cipher.iv // 12 bytes for GCM
        val encrypted = cipher.doFinal(plain)
        return iv + encrypted
    }

    private fun unwrap(blob: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORM)
        val spec = GCMParameterSpec(GCM_TAG_BITS, blob, 0, IV_LENGTH)
        cipher.init(Cipher.DECRYPT_MODE, keystoreKey(), spec)
        return cipher.doFinal(blob, IV_LENGTH, blob.size - IV_LENGTH)
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "anima_soul_wrap"
        const val WRAPPED_FILE = "soul.key"
        const val TRANSFORM = "AES/GCM/NoPadding"
        const val PASSPHRASE_BYTES = 32
        const val IV_LENGTH = 12
        const val GCM_TAG_BITS = 128
    }
}
