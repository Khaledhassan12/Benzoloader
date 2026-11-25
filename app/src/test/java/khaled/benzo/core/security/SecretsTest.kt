package khaled.benzo.core.security

import org.junit.Assert.*
import org.junit.Test
import javax.crypto.KeyGenerator

class SecretsTest {
    private fun codec(): SecretCodec {
        val k = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        return AesGcmCodec { k }
    }

    @Test fun roundTripAndNotPlaintext() {
        val c = codec()
        val s = Secrets.sealWith(c, "sid=abc; token=xyz")!!
        assertTrue(s.startsWith(Secrets.PREFIX)); assertFalse(s.contains("abc"))
        assertEquals(SecretRead.Ok("sid=abc; token=xyz"), Secrets.readWith(c, s))
    }

    @Test fun randomIvGivesDifferentCiphertexts() {
        val c = codec()
        assertNotEquals(Secrets.sealWith(c, "same"), Secrets.sealWith(c, "same"))
    }

    @Test fun nullEmptyAndLegacyPlaintextPassThrough() {
        val c = codec()
        assertNull(Secrets.sealWith(c, null)); assertEquals("", Secrets.sealWith(c, ""))
        assertEquals(SecretRead.Ok("legacy=1"), Secrets.readWith(c, "legacy=1"))
    }

    @Test fun sealIsIdempotent() {
        val c = codec()
        val once = Secrets.sealWith(c, "x")
        assertEquals(once, Secrets.sealWith(c, once))
    }

    @Test fun wrongKeyOrCorruptionIsReportedWithRawValueKept() {
        val s = Secrets.sealWith(codec(), "secret")!!
        assertEquals(SecretRead.Unreadable(s), Secrets.readWith(codec(), s))
        val bad = Secrets.PREFIX + "garbage"
        assertEquals(SecretRead.Unreadable(bad), Secrets.readWith(codec(), bad))
    }

    @Test fun storedTokenHasA12ByteIvAndNoIvIsSuppliedByTheCaller() {
        val token = codec().encrypt("x")
        assertEquals(12, java.util.Base64.getDecoder().decode(token.substringBefore(":")).size)
    }

    @Test fun unicodeSurvives() {
        val c = codec()
        assertEquals(SecretRead.Ok("كوكي=قيمة"), Secrets.readWith(c, Secrets.sealWith(c, "كوكي=قيمة")))
    }
}
