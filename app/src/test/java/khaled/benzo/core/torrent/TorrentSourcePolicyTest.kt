package khaled.benzo.core.torrent

import org.junit.Assert.*
import org.junit.Test
import khaled.benzo.core.torrent.TorrentSourcePolicy as P

class TorrentSourcePolicyTest {
    @Test fun classify() {
        assertEquals(P.Kind.MAGNET, P.classify("magnet:?xt=urn:btih:abc"))
        assertEquals(P.Kind.MAGNET, P.classify("MAGNET:?xt=1"))
        assertEquals(P.Kind.CONTENT, P.classify("content://media/x"))
        assertEquals(P.Kind.FILE, P.classify("file:///sdcard/a.torrent"))
        assertEquals(P.Kind.FILE, P.classify("/sdcard/a.torrent"))
        assertEquals(P.Kind.REJECTED, P.classify("http://x/a.torrent"))
        assertEquals(P.Kind.REJECTED, P.classify("relative/a.torrent"))
    }

    @Test fun sharedStorageAllowedAppDataDenied() {
        val allowed = listOf("/storage/emulated/0")
        val denied = listOf("/data/user/0/khaled.benzo", "/storage/emulated/0/Android/data/khaled.benzo")
        assertTrue(P.isFileAllowed("/storage/emulated/0/Download/a.torrent", allowed, denied))
        assertFalse(P.isFileAllowed("/data/user/0/khaled.benzo/databases/benzo_database.db", allowed, denied))
        assertFalse(P.isFileAllowed("/storage/emulated/0/Android/data/khaled.benzo/files/x", allowed, denied))
        assertFalse(P.isFileAllowed("/etc/hosts", allowed, denied))
    }

    @Test fun prefixIsNotEnoughSiblingDirectoryIsNotInside() {
        assertFalse(P.isInside("/storage/emulated/01/x", "/storage/emulated/0"))
        assertTrue(P.isInside("/storage/emulated/0", "/storage/emulated/0/"))
    }

    @Test fun bencodeShape() {
        assertTrue(P.looksLikeTorrent("d4:infod4:name1:aee".toByteArray()))
        assertFalse(P.looksLikeTorrent("SQLite format 3".toByteArray()))
        assertFalse(P.looksLikeTorrent(ByteArray(0)))
    }

    @Test fun readLimitedStopsAtTheCap() {
        assertNull(P.readLimited(ByteArray(100).inputStream(), 50))
        assertEquals(100, P.readLimited(ByteArray(100).inputStream(), 100)!!.size)
    }
}
