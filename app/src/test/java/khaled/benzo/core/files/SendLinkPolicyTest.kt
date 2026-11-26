package khaled.benzo.core.files

import khaled.benzo.data.model.DownloadItem
import org.junit.Assert.*
import org.junit.Test

/** WRITTEN, NOT RUN. */
class SendLinkPolicyTest {
    private fun d(url: String, torrent: Boolean = false) = DownloadItem(url = url, fileName = "f", savePath = "/p", isTorrent = torrent)
    @Test fun onlyWebLinksOfNonTorrentsDistinct() =
        assertEquals(listOf("https://a.example/f"), SendLinkPolicy.links(listOf(d("https://a.example/f"), d("https://a.example/f"), d("magnet:?xt=1"), d("http://t.example/x", true), d("/storage/x"))))
    @Test fun flagsQueryFragmentAndUserInfo() {
        assertTrue(SendLinkPolicy.mayBePrivate("https://a.example/f?token=1"))
        assertTrue(SendLinkPolicy.mayBePrivate("https://a.example/f#k"))
        assertTrue(SendLinkPolicy.mayBePrivate("https://user:pw@a.example/f"))
        assertFalse(SendLinkPolicy.mayBePrivate("https://a.example/path/file.zip"))
        assertFalse(SendLinkPolicy.mayBePrivate("https://a.example/me@home.zip"))
    }
}
