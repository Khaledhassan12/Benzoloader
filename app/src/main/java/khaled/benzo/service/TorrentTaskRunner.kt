package khaled.benzo.service

import khaled.benzo.core.torrent.TorrentEngine
import khaled.benzo.core.torrent.TorrentMapping
import khaled.benzo.data.model.DownloadItem
import khaled.benzo.data.model.DownloadStatus
import khaled.benzo.data.repository.DownloadRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import java.io.File

/** Drives one torrent through the libtorrent session and mirrors its real stats into the download row. No service state. */
class TorrentTaskRunner(private val repository: DownloadRepository, private val filesDir: File) {

    /** Returns true when the torrent finished, false when it failed. Cancellation is rethrown. */
    suspend fun run(item: DownloadItem, onRunning: (DownloadItem) -> Unit): Boolean {
        val id = item.id
        val hash = item.torrentInfoHash ?: return false
        var failed = false
        try {
            val engine = TorrentEngine.instance
            if (engine.hasTorrent(hash)) {
                engine.resumeTorrent(hash)
            } else {
                val dir = File(item.savePath)
                val tdir = File(filesDir, "torrents")
                val tf = File(tdir, "$hash.torrent")
                if (tf.exists()) {
                    val prios = TorrentMapping.decodePriorities(File(tdir, "$hash.prio").takeIf { it.exists() }?.readText())
                    engine.addTorrentFile(tf, dir, prios)
                } else if (TorrentMapping.isMagnet(item.url)) {
                    if (engine.addMagnet(item.url, dir) == null) failed = true
                } else failed = true
            }
            while (!failed && currentCoroutineContext().isActive) {
                val st = engine.getStats(hash)
                if (st == null) { failed = true; break }
                repository.updateProgress(id, st.downloaded, st.downloadSpeed)
                onRunning(item.copy(downloadedBytes = st.downloaded, speed = st.downloadSpeed,
                    totalBytes = if (st.total > 0) st.total else item.totalBytes, status = DownloadStatus.RUNNING))
                if (st.isFinished) break
                delay(1000)
            }
            currentCoroutineContext().ensureActive() // a cancelled job must never be reported as finished
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            failed = true
        }
        return !failed
    }
}
