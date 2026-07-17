package app.anima.core.data.crypto

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * The Keystore wrap path on a real AndroidKeyStore (GMD): generation,
 * unwrap stability, and — the ADR-003 guarantee — corrupt `soul.key`
 * failing loudly instead of silently regenerating over an existing soul.
 */
@RunWith(AndroidJUnit4::class)
class KeystoreSoulKeySourceDeviceTest {
    private lateinit var context: Context
    private lateinit var keyFile: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        keyFile = File(context.noBackupFilesDir, "soul.key")
        keyFile.delete()
    }

    @After
    fun tearDown() {
        keyFile.delete()
    }

    @Test
    fun generates_32_bytes_and_unwraps_the_same_passphrase_across_instances() {
        val first = KeystoreSoulKeySource(context).passphrase()
        assertThat(first).hasLength(32)
        assertThat(keyFile.exists()).isTrue()
        // A fresh instance must unwrap the identical passphrase (process
        // restart simulation) — this exercises the real Keystore decrypt.
        val second = KeystoreSoulKeySource(context).passphrase()
        assertThat(second).isEqualTo(first)
    }

    @Test
    fun wrapped_file_does_not_contain_the_plaintext_passphrase() {
        val plain = KeystoreSoulKeySource(context).passphrase()
        val onDisk = keyFile.readBytes()
        assertThat(onDisk.toList()).doesNotContain(plain.toList())
        // 12-byte IV + ciphertext + 16-byte GCM tag.
        assertThat(onDisk.size).isEqualTo(12 + 32 + 16)
    }

    @Test
    fun truncated_soul_key_fails_loudly() {
        KeystoreSoulKeySource(context).passphrase()
        keyFile.writeBytes(byteArrayOf(1, 2, 3))
        assertThrows(IllegalStateException::class.java) {
            KeystoreSoulKeySource(context).passphrase()
        }
    }

    @Test
    fun tampered_soul_key_fails_the_gcm_tag_not_silently() {
        KeystoreSoulKeySource(context).passphrase()
        val blob = keyFile.readBytes()
        blob[blob.size - 1] = (blob.last().toInt() xor 0xFF).toByte()
        keyFile.writeBytes(blob)
        // AEADBadTagException (or a GeneralSecurityException subclass) — the
        // contract is THROW; regenerating would destroy an existing soul.
        assertThrows(Exception::class.java) {
            KeystoreSoulKeySource(context).passphrase()
        }
        // And the corrupt file must still be there — no destructive rewrite.
        assertThat(keyFile.readBytes()).isEqualTo(blob)
    }
}
