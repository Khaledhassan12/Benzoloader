package khaled.benzo.core.files

import khaled.benzo.data.model.DownloadItem
import khaled.benzo.data.model.DownloadStatus
import org.junit.Assert.*
import org.junit.Test

/** WRITTEN, NOT RUN. */
class FileActionPolicyTest {
    private fun it(id: Long, st: DownloadStatus, torrent: Boolean = false) = DownloadItem(id = id, url = "u", fileName = "f$id", savePath = "/p", status = st, isTorrent = torrent)
    @Test fun onlyFinishedExistingNonTorrentFiles() {
        val all = listOf(it(1, DownloadStatus.COMPLETED), it(2, DownloadStatus.RUNNING), it(3, DownloadStatus.COMPLETED, torrent = true), it(4, DownloadStatus.COMPLETED))
        assertEquals(listOf(1L), FileActionPolicy.usable(all) { d -> d.id == 1L }.map { d -> d.id })
    }
    @Test fun openNeedsExactlyOneSelected() {
        val a = it(1, DownloadStatus.COMPLETED); val b = it(2, DownloadStatus.COMPLETED)
        assertEquals(a, FileActionPolicy.openTarget(listOf(a)) { true })
        assertNull(FileActionPolicy.openTarget(listOf(a, b)) { true })
        assertNull(FileActionPolicy.openTarget(listOf(a)) { false })
    }
}
