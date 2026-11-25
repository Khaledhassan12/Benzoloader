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
    private val seedAfterFinish: () -> Boolean,
    private val onSeeding: (hash: String) -> Unit = {}
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
            var metadataSaved = false
            while (!failed && currentCoroutineContext().isActive) {
                val st = engine.getStats(hash)
                when (grace.onPoll(st != null)) {
                    StartGrace.Verdict.FAIL -> { failed = true; break }
                    StartGrace.Verdict.WAIT -> { delay(1000); continue } // the session adds torrents asynchronously
                    StartGrace.Verdict.FOUND -> {}
                }
                if (st == null) { failed = true; break }
                if (!optionsApplied) optionsApplied = engine.setSequential(hash, sequential())
                if (!metadataSaved) metadataSaved = saveMetadataOnce(engine, hash)
                repository.updateProgress(id, st.downloaded, st.downloadSpeed)
                onRunning(item.copy(downloadedBytes = st.downloaded, speed = st.downloadSpeed,
                    totalBytes = if (st.total > 0) st.total else item.totalBytes, status = DownloadStatus.RUNNING))
                if (st.isFinished) {
                    if (!seedAfterFinish()) engine.pauseTorrent(hash) // user chose not to keep uploading
                    else onSeeding(hash) // the seeding monitor applies the ratio and time limits
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

    /**
     * A magnet torrent has no .torrent file until its metadata arrives. Once it does, keep it next to the others so the
     * next start re-adds it from the file instead of asking the swarm again. Returns true when nothing more is to do.
     */
    private fun saveMetadataOnce(engine: TorrentEngine, hash: String): Boolean {
        val tf = File(File(filesDir, "torrents"), "$hash.torrent")
        if (tf.exists()) return true
        val bytes = engine.metadataBytes(hash) ?: return false
        return try {
            tf.parentFile?.mkdirs()
            val tmp = File(tf.parentFile, "$hash.torrent.tmp")
            tmp.writeBytes(bytes)
            tmp.renameTo(tf) || run { tmp.delete(); false }
        } catch (e: java.io.IOException) { false }
    }

    private companion object { const val START_GRACE_POLLS = 20 }
}
