package khaled.benzo.core.housekeeping

import khaled.benzo.data.model.DownloadItem
import khaled.benzo.data.model.DownloadStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ClearFinishedPolicyTest {
    private fun d(id: Long, st: DownloadStatus, torrent: Boolean = false) =
        DownloadItem(id = id, url = "https://x/$id", fileName = "f$id", savePath = "/x", status = st, isTorrent = torrent)

    private val items = listOf(
        d(1, DownloadStatus.COMPLETED), d(2, DownloadStatus.RUNNING), d(3, DownloadStatus.ERROR),
        d(4, DownloadStatus.COMPLETED, torrent = true), d(5, DownloadStatus.PAUSED), d(6, DownloadStatus.COMPLETED)
    )

    @Test fun offClearsNothing() = assertEquals(emptyList<Long>(), ClearFinishedPolicy.idsToClear(items, false))

    @Test fun onClearsOnlyCompletedNonTorrents() = assertEquals(listOf(1L, 6L), ClearFinishedPolicy.idsToClear(items, true))
}
