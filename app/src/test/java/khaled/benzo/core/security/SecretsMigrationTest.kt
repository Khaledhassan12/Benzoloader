package khaled.benzo.core.security

import org.junit.Assert.*
import org.junit.Test
import javax.crypto.KeyGenerator

class SecretsMigrationTest {
    private fun codec(): SecretCodec {
        val k = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        return AesGcmCodec { k }
    }

    @Test fun plaintextRowIsSealedAndReadsBack() {
        val c = codec()
        val n = SecretsMigration.plan(c, "sid=1", "{\"A\":\"B\"}")!!
        assertTrue(n.all { it!!.startsWith(Secrets.PREFIX) })
        assertEquals(SecretRead.Ok("sid=1"), Secrets.readWith(c, n[0]))
    }

    @Test fun alreadySealedOrEmptyRowsAreSkipped() {
        val c = codec()
        val sealed = Secrets.sealWith(c, "x")
        assertNull(SecretsMigration.plan(c, sealed, null))
        assertNull(SecretsMigration.plan(c, "", null))
    }

    @Test fun mixedRowSealsOnlyThePlainColumn() {
        val c = codec()
        val sealed = Secrets.sealWith(c, "x")
        val n = SecretsMigration.plan(c, "plain", sealed)!!
        assertTrue(n[0]!!.startsWith(Secrets.PREFIX)); assertEquals(sealed, n[1])
    }

    @Test fun repeatingAfterSuccessChangesNothing() {
        val c = codec()
        val n = SecretsMigration.plan(c, "a", "b")!!
        assertNull(SecretsMigration.plan(c, n[0], n[1]))
    }

    @Test fun failingCodecLeavesRowUntouched() {
        val bad = object : SecretCodec {
            override fun encrypt(plain: String): String = throw IllegalStateException("keystore")
            override fun decrypt(token: String): String = plain(token)
            private fun plain(t: String) = t
        }
        assertNull(SecretsMigration.plan(bad, "a", null))
    }

    @Test fun codecThatDoesNotRoundTripIsRejected() {
        val lossy = object : SecretCodec {
            override fun encrypt(plain: String) = "zzz"
            override fun decrypt(token: String) = "different"
        }
        assertNull(SecretsMigration.plan(lossy, "a", null))
    }
}
