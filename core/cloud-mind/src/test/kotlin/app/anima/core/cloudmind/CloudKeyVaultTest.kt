package app.anima.core.cloudmind

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.anima.core.model.CloudMindConfig
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Vault file handling + config store on Robolectric with a transparent-XOR
 * fake cipher (AndroidKeyStore itself is device-only; the real wrap runs in
 * the GMD suite pattern established for the soul key). What matters here:
 * the plaintext never lands on disk, zeroing happens, hasKey tracks the
 * file, and DataStore never sees the key.
 */
@RunWith(RobolectricTestRunner::class)
class CloudKeyVaultTest {
    private class XorCipher : SecretCipher {
        override fun wrap(plain: ByteArray) = plain.map { (it.toInt() xor 0x5A).toByte() }.toByteArray()

        override fun unwrap(blob: ByteArray) = blob.map { (it.toInt() xor 0x5A).toByte() }.toByteArray()
    }

    private lateinit var context: Context
    private lateinit var vault: CloudKeyVault

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        vault = CloudKeyVault(context, XorCipher())
        vault.clear()
    }

    @Test
    fun `round trip stores wrapped, reads plaintext, zeroes the input`() {
        val input = "sk-test-key-123".toByteArray()
        vault.store(input)
        // The array handed in is scrubbed once wrapped.
        assertThat(input.all { it == 0.toByte() }).isTrue()
        assertThat(vault.hasKey()).isTrue()
        assertThat(vault.read()!!.decodeToString()).isEqualTo("sk-test-key-123")
        // On-disk bytes are not the plaintext.
        val onDisk = File(context.noBackupFilesDir, "cloud.key").readBytes()
        assertThat(onDisk.decodeToString()).isNotEqualTo("sk-test-key-123")
    }

    @Test
    fun `clear removes the key and read returns null`() {
        vault.store("sk-x".toByteArray())
        vault.clear()
        assertThat(vault.hasKey()).isFalse()
        assertThat(vault.read()).isNull()
    }

    @Test
    fun `config store reflects key presence but never stores the key itself`() =
        runTest {
            val store = CloudMindConfigStore(context, vault)
            store.setEndpoint("https://api.example.com/v1/", "small-model")
            store.setEnabled(true)
            store.storeKey("sk-secret".toByteArray())
            val config = store.current()
            assertThat(config).isEqualTo(
                CloudMindConfig(
                    enabled = true,
                    baseUrl = "https://api.example.com/v1",
                    model = "small-model",
                    hasKey = true,
                ),
            )
            assertThat(config.usable).isTrue()
            // The DataStore file must not contain the secret.
            val dsDir = File(context.filesDir, "datastore")
            val leaked =
                dsDir
                    .walkTopDown()
                    .filter { it.isFile }
                    .any { it.readBytes().decodeToString().contains("sk-secret") }
            assertThat(leaked).isFalse()
            // Disabling via clearKeyAndDisable drops both key and enablement.
            store.clearKeyAndDisable()
            val after = store.current()
            assertThat(after.hasKey).isFalse()
            assertThat(after.enabled).isFalse()
            assertThat(after.usable).isFalse()
        }
}
