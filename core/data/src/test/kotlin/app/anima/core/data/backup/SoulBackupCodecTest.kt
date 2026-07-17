package app.anima.core.data.backup

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class SoulBackupCodecTest {
    @Test
    fun `seal then open round-trips the payload`() {
        val payload = """{"format":"anima-soul","facts":[1,2,3]}""".toByteArray()
        val sealed = SoulBackupCodec.seal(payload, "correct horse".toCharArray())
        val opened = SoulBackupCodec.open(sealed, "correct horse".toCharArray())
        assertThat(opened).isEqualTo(payload)
    }

    @Test
    fun `wrong passphrase fails loudly not garbage`() {
        val sealed = SoulBackupCodec.seal("secret".toByteArray(), "right".toCharArray())
        assertThrows(SoulBackupCodec.WrongPassphraseOrCorrupt::class.java) {
            SoulBackupCodec.open(sealed, "wrong".toCharArray())
        }
    }

    @Test
    fun `flipped ciphertext byte is detected by GCM`() {
        val sealed = SoulBackupCodec.seal("secret".toByteArray(), "pass".toCharArray())
        sealed[sealed.size - 1] = (sealed.last().toInt() xor 1).toByte()
        assertThrows(SoulBackupCodec.WrongPassphraseOrCorrupt::class.java) {
            SoulBackupCodec.open(sealed, "pass".toCharArray())
        }
    }

    @Test
    fun `truncated or foreign file is rejected`() {
        assertThrows(SoulBackupCodec.WrongPassphraseOrCorrupt::class.java) {
            SoulBackupCodec.open(ByteArray(10), "pass".toCharArray())
        }
        assertThrows(SoulBackupCodec.WrongPassphraseOrCorrupt::class.java) {
            SoulBackupCodec.open("not a backup at all, just text".toByteArray(), "pass".toCharArray())
        }
    }

    @Test
    fun `header advertises the OWASP iteration floor`() {
        // salt starts after "ANIMASOUL"+version; iterations after 16-byte salt.
        val sealed = SoulBackupCodec.seal("x".toByteArray(), "p".toCharArray())
        val offset = "ANIMASOUL".length + 1 + 16
        val iterations =
            ((sealed[offset].toInt() and 0xFF) shl 24) or
                ((sealed[offset + 1].toInt() and 0xFF) shl 16) or
                ((sealed[offset + 2].toInt() and 0xFF) shl 8) or
                (sealed[offset + 3].toInt() and 0xFF)
        assertThat(iterations).isEqualTo(SoulBackupCodec.ITERATIONS)
        assertThat(iterations).isAtLeast(600_000)
    }
}
