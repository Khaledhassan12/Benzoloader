package khaled.benzo.core.torrent

import khaled.benzo.data.model.DownloadItem
import org.junit.Assert.*
import org.junit.Test

/** WRITTEN, NOT RUN. */
class TorrentActionPolicyTest {
    private fun d(id: Long, t: Boolean, h: String?) = DownloadItem(id = id, url = "u", fileName = "f", savePath = "/p", isTorrent = t, torrentInfoHash = h)
    @Test fun onlyTorrentsWithAHash() {
        val all = listOf(d(1, true, "aa"), d(2, false, null), d(3, true, null), d(4, true, " "))
        assertEquals(listOf(1L), TorrentActionPolicy.targets(all).map { it.id })
        assertTrue(TorrentActionPolicy.showActions(all)); assertFalse(TorrentActionPolicy.showActions(all.drop(1)))
    }
}
