package khaled.benzo.core.torrent

import org.junit.Assert.*
import org.junit.Test

class TorrentEncryptionTest {
    @Test fun indexRoundTrip() = TorrentEncryption.values().forEach { assertEquals(it, TorrentEncryption.fromIndex(it.index)) }
    @Test fun unknownIsPrefer() = assertEquals(TorrentEncryption.PREFER, TorrentEncryption.fromIndex(9))
    @Test fun onlyDisableDims() = assertEquals(listOf(false, false, true), TorrentEncryption.values().map { it.dimsConnectionRows })
}
