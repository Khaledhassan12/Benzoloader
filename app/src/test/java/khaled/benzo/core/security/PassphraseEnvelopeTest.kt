package khaled.benzo.core.security

import org.junit.Assert.*
import org.junit.Test

class PassphraseEnvelopeTest {
    private val it = 1000 // low count keeps the test fast
    @Test fun roundTrip() {
        val t = PassphraseEnvelope.seal("secret\nline", "pw1".toCharArray(), it)
        assertFalse(t.contains("secret"))
        assertEquals("secret\nline", PassphraseEnvelope.open(t, "pw1".toCharArray()))
    }
    @Test fun wrongPassphraseFails() = assertNull(PassphraseEnvelope.open(PassphraseEnvelope.seal("x", "a".toCharArray(), it), "b".toCharArray()))
    @Test fun tamperFails() {
        val t = PassphraseEnvelope.seal("x", "a".toCharArray(), it)
        val bad = t.dropLast(6) + (if (t.takeLast(6) == "AAAAAA") "BBBBBB" else "AAAAAA")
        assertNull(PassphraseEnvelope.open(bad, "a".toCharArray()))
    }
    @Test fun twoSealsDiffer() = assertNotEquals(PassphraseEnvelope.seal("x", "a".toCharArray(), it), PassphraseEnvelope.seal("x", "a".toCharArray(), it))
    @Test fun absurdIterationsRefused() = assertNull(PassphraseEnvelope.open("BZENV1:999999999:AA:AA:AA", "a".toCharArray()))
}
