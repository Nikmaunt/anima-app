package app.anima.core.data.backup

import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * The "soul migration" envelope (v0.2, research-v2 §B.3): a whole soul in one
 * passphrase-protected file.
 *
 * Layout: magic "ANIMASOUL" ‖ version(1B) ‖ salt(16B) ‖ iterations(int32 BE) ‖
 * IV(12B) ‖ AES-256-GCM ciphertext+tag. KDF params live in the header so the
 * iteration count can rise without breaking old files.
 *
 * KDF: PBKDF2-HMAC-SHA256, 600 000 iterations (OWASP 2026 floor for PBKDF2).
 * Argon2id would be stronger but needs a native dependency (argon2kt, stale
 * since 2024) — rejected; zero-dependency javax.crypto wins for this repo's
 * threat model (offline brute force on an exported file).
 *
 * Pure JVM: unit-testable without a device.
 */
object SoulBackupCodec {
    const val ITERATIONS = 600_000

    private val MAGIC = "ANIMASOUL".toByteArray(Charsets.US_ASCII)
    private const val VERSION: Byte = 1
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val KEY_BITS = 256
    private const val TAG_BITS = 128
    private const val INT_BYTES = 4

    class WrongPassphraseOrCorrupt(
        cause: Throwable? = null,
    ) : Exception("backup cannot be opened: wrong passphrase or damaged file", cause)

    fun seal(
        payload: ByteArray,
        passphrase: CharArray,
        rng: SecureRandom = SecureRandom(),
    ): ByteArray {
        val salt = ByteArray(SALT_BYTES).also(rng::nextBytes)
        val iv = ByteArray(IV_BYTES).also(rng::nextBytes)
        val key = deriveKey(passphrase, salt, ITERATIONS)
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
            val ciphertext = cipher.doFinal(payload)
            return MAGIC + byteArrayOf(VERSION) + salt + intToBytes(ITERATIONS) + iv + ciphertext
        } finally {
            Arrays.fill(key, 0)
        }
    }

    fun open(
        blob: ByteArray,
        passphrase: CharArray,
    ): ByteArray {
        var offset = 0
        val headerBytes = MAGIC.size + 1 + SALT_BYTES + INT_BYTES + IV_BYTES
        if (blob.size <= headerBytes) throw WrongPassphraseOrCorrupt()
        if (!blob.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) throw WrongPassphraseOrCorrupt()
        offset += MAGIC.size
        if (blob[offset] != VERSION) throw WrongPassphraseOrCorrupt()
        offset += 1
        val salt = blob.copyOfRange(offset, offset + SALT_BYTES)
        offset += SALT_BYTES
        val iterations = bytesToInt(blob, offset)
        offset += INT_BYTES
        if (iterations < MIN_ACCEPTED_ITERATIONS) throw WrongPassphraseOrCorrupt()
        val iv = blob.copyOfRange(offset, offset + IV_BYTES)
        offset += IV_BYTES
        val key = deriveKey(passphrase, salt, iterations)
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
            return runCatching { cipher.doFinal(blob, offset, blob.size - offset) }
                .getOrElse { throw WrongPassphraseOrCorrupt(it) }
        } finally {
            Arrays.fill(key, 0)
        }
    }

    private fun deriveKey(
        passphrase: CharArray,
        salt: ByteArray,
        iterations: Int,
    ): ByteArray {
        val spec = PBEKeySpec(passphrase, salt, iterations, KEY_BITS)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun intToBytes(value: Int): ByteArray =
        byteArrayOf(
            (value ushr 24).toByte(),
            (value ushr 16).toByte(),
            (value ushr 8).toByte(),
            value.toByte(),
        )

    private fun bytesToInt(
        source: ByteArray,
        offset: Int,
    ): Int =
        ((source[offset].toInt() and 0xFF) shl 24) or
            ((source[offset + 1].toInt() and 0xFF) shl 16) or
            ((source[offset + 2].toInt() and 0xFF) shl 8) or
            (source[offset + 3].toInt() and 0xFF)

    /** Downgrade guard: a tampered header cannot ask for 1 iteration. */
    private const val MIN_ACCEPTED_ITERATIONS = 100_000
}
