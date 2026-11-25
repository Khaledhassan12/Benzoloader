package khaled.benzo.core.torrent

import org.junit.Assert.*
import org.junit.Test

class TorrentMappingTest {
    @Test fun clampsPriorities() {
        assertEquals(0, TorrentMapping.clampPriority(-3))
        assertEquals(7, TorrentMapping.clampPriority(99))
        assertEquals(4, TorrentMapping.clampPriority(4))
    }
    @Test fun unselectedFilesAreSkipped() {
        assertEquals(listOf(1, 0, 7), TorrentMapping.effectivePriorities(listOf(true, false, true), listOf(1, 7, 7)))
    }
    @Test fun missingPriorityDefaultsToNormal() {
        assertEquals(listOf(1), TorrentMapping.effectivePriorities(listOf(true), emptyList()))
    }
    @Test fun magnetDetection() {
        assertTrue(TorrentMapping.isMagnet(" MAGNET:?xt=urn:btih:abc"))
        assertFalse(TorrentMapping.isMagnet("http://x/y.torrent"))
    }
    @Test fun hexHashExtracted() {
        val h = "0123456789abcdef0123456789ABCDEF01234567"
        assertEquals(h.lowercase(), TorrentMapping.magnetInfoHash("magnet:?xt=urn:btih:$h&dn=x"))
    }
    @Test fun base32HashConvertedToHex() {
        // 20 zero bytes -> 32 'A'
        assertEquals("0".repeat(40), TorrentMapping.magnetInfoHash("magnet:?xt=urn:btih:" + "A".repeat(32)))
    }
    @Test fun badMagnetRejected() {
        assertNull(TorrentMapping.magnetInfoHash("magnet:?dn=x"))
        assertNull(TorrentMapping.magnetInfoHash("magnet:?xt=urn:btih:zz"))
    }
    @Test fun stateNames() {
        assertEquals("PAUSED", TorrentMapping.stateName("DOWNLOADING", true, false))
        assertEquals("COMPLETED", TorrentMapping.stateName("SEEDING", false, true))
        assertEquals("FETCHING_METADATA", TorrentMapping.stateName("DOWNLOADING_METADATA", false, false))
        assertEquals("DOWNLOADING", TorrentMapping.stateName("DOWNLOADING", false, false))
    }
}
