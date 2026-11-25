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
        assertEquals("sid=abc; token=xyz", Secrets.openWith(c, s))
    }

    @Test fun randomIvGivesDifferentCiphertexts() {
        val c = codec()
        assertNotEquals(Secrets.sealWith(c, "same"), Secrets.sealWith(c, "same"))
    }

    @Test fun nullEmptyAndLegacyPlaintextPassThrough() {
        val c = codec()
        assertNull(Secrets.sealWith(c, null)); assertEquals("", Secrets.sealWith(c, ""))
        assertEquals("legacy=1", Secrets.openWith(c, "legacy=1"))
    }

    @Test fun sealIsIdempotent() {
        val c = codec()
        val once = Secrets.sealWith(c, "x")
        assertEquals(once, Secrets.sealWith(c, once))
    }

    @Test fun wrongKeyOrCorruptionReadsAsNull() {
        val s = Secrets.sealWith(codec(), "secret")
        assertNull(Secrets.openWith(codec(), s))
        assertNull(Secrets.openWith(codec(), Secrets.PREFIX + "garbage"))
    }

    @Test fun unicodeSurvives() {
        val c = codec()
        assertEquals("كوكي=قيمة", Secrets.openWith(c, Secrets.sealWith(c, "كوكي=قيمة")))
    }
}
