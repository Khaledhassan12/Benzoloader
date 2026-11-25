package khaled.benzo.core.torrent

import org.junit.Assert.*
import org.junit.Test

class TorrentCapabilityTest {
    @Test fun workingFactoryIsCreatedOnceAndHasNoDiagnostic() {
        var calls = 0
        val c = TorrentCapability { calls++; "engine" }
        assertEquals("engine", c.engineOrNull())
        assertEquals("engine", c.engineOrNull())
        assertEquals(1, calls)
        assertNull(c.diagnostic())
    }

    @Test fun failedNativeLoadGivesNullAndAReasonAndIsNotRetried() {
        var calls = 0
        val c = TorrentCapability<String> { calls++; throw UnsatisfiedLinkError("no jlibtorrent for this abi") }
        assertNull(c.engineOrNull())
        assertNull(c.engineOrNull())
        assertEquals(1, calls)
        assertEquals("UnsatisfiedLinkError: no jlibtorrent for this abi", c.diagnostic())
    }

    @Test fun initializerErrorIsAlsoCaught() {
        val c = TorrentCapability<String> { throw ExceptionInInitializerError("boom") }
        assertNull(c.engineOrNull())
        assertNotNull(c.diagnostic())
    }

    @Test fun otherErrorsAreNotHidden() {
        val c = TorrentCapability<String> { throw IllegalStateException("bug") }
        try { c.engineOrNull(); fail("must propagate") } catch (e: IllegalStateException) { assertEquals("bug", e.message) }
    }
}
