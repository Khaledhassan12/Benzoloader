package khaled.benzo.service

import khaled.benzo.core.torrent.TorrentEngine
import khaled.benzo.core.torrent.StartGrace
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
class TorrentTaskRunner(
    private val repository: DownloadRepository,
    private val filesDir: File,
    private val sequential: () -> Boolean,
    private val seedAfterFinish: () -> Boolean
) {

    /** Returns true when the torrent finished, false when it failed. Cancellation is rethrown. */
    suspend fun run(item: DownloadItem, onRunning: (DownloadItem) -> Unit): Boolean {
        val id = item.id
        val hash = item.torrentInfoHash ?: return false
        var failed = false
        try {
            val engine = TorrentEngine.orNull() ?: return false // library cannot load: this torrent fails, HTTP downloads go on
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
            val grace = StartGrace(START_GRACE_POLLS)
            var optionsApplied = false
            while (!failed && currentCoroutineContext().isActive) {
                val st = engine.getStats(hash)
                when (grace.onPoll(st != null)) {
                    StartGrace.Verdict.FAIL -> { failed = true; break }
                    StartGrace.Verdict.WAIT -> { delay(1000); continue } // the session adds torrents asynchronously
                    StartGrace.Verdict.FOUND -> {}
                }
                if (st == null) { failed = true; break }
                if (!optionsApplied) optionsApplied = engine.setSequential(hash, sequential())
                repository.updateProgress(id, st.downloaded, st.downloadSpeed)
                onRunning(item.copy(downloadedBytes = st.downloaded, speed = st.downloadSpeed,
                    totalBytes = if (st.total > 0) st.total else item.totalBytes, status = DownloadStatus.RUNNING))
                if (st.isFinished) {
                    if (!seedAfterFinish()) engine.pauseTorrent(hash) // user chose not to keep uploading
                    break
                }
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

    private companion object { const val START_GRACE_POLLS = 20 }
}
