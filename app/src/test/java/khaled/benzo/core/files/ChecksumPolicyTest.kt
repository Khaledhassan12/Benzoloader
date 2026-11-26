package khaled.benzo.core.files

import org.junit.Assert.*
import org.junit.Test

/** WRITTEN, NOT RUN. */
class ChecksumPolicyTest {
    @Test fun normalizeAcceptsKnownLengths() {
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", ChecksumPolicy.normalize(" D41D8CD98F00B204E9800998ECF8427E "))
        assertNull(ChecksumPolicy.normalize("abc"))
        assertNull(ChecksumPolicy.normalize("z".repeat(32)))
        assertNull(ChecksumPolicy.normalize(null))
    }
    @Test fun algorithmByLength() {
        assertEquals("MD5", ChecksumPolicy.algorithm("a".repeat(32)))
        assertEquals("SHA-1", ChecksumPolicy.algorithm("a".repeat(40)))
        assertEquals("SHA-256", ChecksumPolicy.algorithm("a".repeat(64)))
    }
    @Test fun unreadableIsNotMismatch() {
        val missing = java.io.File("/nonexistent/none.bin")
        assertEquals(ChecksumPolicy.Result.UNREADABLE, ChecksumPolicy.check(missing, "0".repeat(32)))
    }
    @Test fun matchesEmptyFileMd5() {
        val f = java.io.File.createTempFile("cs_t", ".bin")
        try { assertTrue(ChecksumPolicy.matches(f, "d41d8cd98f00b204e9800998ecf8427e")); assertFalse(ChecksumPolicy.matches(f, "0".repeat(32))) }
        finally { f.delete() }
    }
}
