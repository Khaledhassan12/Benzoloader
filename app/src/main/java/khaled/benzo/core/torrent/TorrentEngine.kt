package khaled.benzo.core.torrent

import android.content.Context
import com.frostwire.jlibtorrent.Priority
import com.frostwire.jlibtorrent.SessionManager
import com.frostwire.jlibtorrent.Sha1Hash
import com.frostwire.jlibtorrent.TorrentInfo
import com.frostwire.jlibtorrent.swig.remove_flags_t
import khaled.benzo.data.model.TorrentFileEntry
import khaled.benzo.data.model.TorrentItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Real BitTorrent engine on top of jlibtorrent (libtorrent). Nothing here is simulated:
 * every number comes from the session. Compiled only; not yet run on a device.
 */
class TorrentEngine(context: Context? = null) {

    private val session = SessionManager()
    private val lock = Any()

    fun initEngine(@Suppress("UNUSED_PARAMETER") storageDir: File? = null) {
        synchronized(lock) {
            if (!session.isRunning) session.start()
        }
    }

    private fun ensureStarted() = initEngine(null)

    fun toItem(ti: TorrentInfo, savePath: String): TorrentItem {
        val fs = ti.files()
        val files = (0 until ti.numFiles()).map { i ->
            TorrentFileEntry(i, fs.filePath(i), fs.fileSize(i), true, TorrentMapping.PRIORITY_NORMAL)
        }
        return TorrentItem(
            infoHash = ti.infoHash().toHex(),
            name = ti.name(),
            totalSize = ti.totalSize(),
            downloadedSize = 0L, uploadSize = 0L, downloadSpeed = 0L, uploadSpeed = 0L,
            seeds = 0, peers = 0, state = "READY", savePath = savePath, files = files
        )
    }

    /** Parses a .torrent file. Returns null when the file is not a valid torrent. */
    suspend fun parseTorrentFile(torrentFile: File): TorrentItem? = withContext(Dispatchers.IO) {
        try {
            val ti = TorrentInfo(torrentFile)
            if (!ti.isValid) null else toItem(ti, torrentFile.parent ?: "")
        } catch (e: Exception) {
            null
        }
    }

    /** Downloads magnet metadata from the swarm. Returns raw torrent bytes or null on timeout. */
    suspend fun fetchMagnetMetadata(magnetUri: String, timeoutSeconds: Int = 30): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                ensureStarted()
                session.fetchMagnet(magnetUri, timeoutSeconds)
            } catch (e: Exception) {
                null
            }
        }

    suspend fun parseMagnet(magnetUri: String, timeoutSeconds: Int = 30): TorrentItem? =
        withContext(Dispatchers.IO) {
            val bytes = fetchMagnetMetadata(magnetUri, timeoutSeconds) ?: return@withContext null
            try {
                toItem(TorrentInfo.bdecode(bytes), "")
            } catch (e: Exception) {
                null
            }
        }

    /** Starts a download from .torrent bytes. priorities: per-file 0 skip, 1 normal ... 7 high. */
    fun addTorrentBytes(torrentBytes: ByteArray, saveDir: File, priorities: List<Int>? = null): String {
        ensureStarted()
        saveDir.mkdirs()
        val ti = TorrentInfo.bdecode(torrentBytes)
        val prios = priorities?.let { list ->
            Array(ti.numFiles()) { i -> toPriority(list.getOrElse(i) { 1 }) }
        }
        session.download(ti, saveDir, null, prios, null)
        return ti.infoHash().toHex()
    }

    fun addTorrentFile(torrentFile: File, saveDir: File, priorities: List<Int>? = null): String =
        addTorrentBytes(torrentFile.readBytes(), saveDir, priorities)

    /** Adds a magnet link; metadata is fetched by the session after adding. Returns the info hash. */
    fun addMagnet(magnetUri: String, saveDir: File): String? {
        val hash = TorrentMapping.magnetInfoHash(magnetUri) ?: return null
        ensureStarted()
        saveDir.mkdirs()
        session.download(magnetUri, saveDir)
        return hash
    }

    private fun handle(infoHash: String) = session.find(Sha1Hash(infoHash))

    fun hasTorrent(infoHash: String): Boolean = session.isRunning && handle(infoHash)?.isValid == true

    fun pauseTorrent(infoHash: String) { handle(infoHash)?.takeIf { it.isValid }?.pause() }
    fun resumeTorrent(infoHash: String) { handle(infoHash)?.takeIf { it.isValid }?.resume() }

    fun setFilePriorities(infoHash: String, priorities: List<Int>) {
        handle(infoHash)?.takeIf { it.isValid }?.prioritizeFiles(
            Array(priorities.size) { toPriority(priorities[it]) }
        )
    }

    fun removeTorrent(infoHash: String, deleteFiles: Boolean) {
        val h = handle(infoHash)?.takeIf { it.isValid } ?: return
        if (deleteFiles) session.remove(h, SESSION_DELETE_FILES) else session.remove(h)
    }

    /** Live snapshot from libtorrent, or null when the torrent is not in the session. */
    fun getStats(infoHash: String): TorrentStats? {
        val h = handle(infoHash)?.takeIf { it.isValid } ?: return null
        val st = h.status()
        val total = st.totalWanted()
        val done = st.totalWantedDone()
        val progress = if (total > 0) ((done * 100) / total).toInt().coerceIn(0, 100) else 0
        return TorrentStats(
            downloaded = done,
            total = total,
            downloadSpeed = st.downloadPayloadRate().toLong(),
            uploadSpeed = st.uploadPayloadRate().toLong(),
            seeds = st.numSeeds(),
            peers = st.numPeers(),
            progress = progress,
            isFinished = st.isFinished,
            state = TorrentMapping.stateName(st.state().name, h.flags().and_(PAUSED_FLAG).nonZero(), st.isFinished)
        )
    }

    fun stop() = synchronized(lock) { if (session.isRunning) session.stop() }

    private fun toPriority(p: Int): Priority = when (TorrentMapping.clampPriority(p)) {
        0 -> Priority.IGNORE
        1 -> Priority.NORMAL
        2 -> Priority.TWO
        3 -> Priority.THREE
        4 -> Priority.FOUR
        5 -> Priority.FIVE
        6 -> Priority.SIX
        else -> Priority.SEVEN
    }

    data class TorrentStats(
        val downloaded: Long,
        val total: Long,
        val downloadSpeed: Long,
        val uploadSpeed: Long,
        val seeds: Int,
        val peers: Int,
        val progress: Int,
        val isFinished: Boolean,
        val state: String = "UNKNOWN"
    )

    companion object {
        private val capability = TorrentCapability { TorrentEngine() }
        /** The engine, or null when the native torrent library cannot load on this device. */
        fun orNull(): TorrentEngine? = capability.engineOrNull()
        /** Why torrents are unavailable (null when they work). Shown instead of crashing. */
        fun unavailableReason(): String? { capability.engineOrNull(); return capability.diagnostic() }
        private val SESSION_DELETE_FILES: remove_flags_t = com.frostwire.jlibtorrent.SessionHandle.DELETE_FILES
        private val PAUSED_FLAG = com.frostwire.jlibtorrent.TorrentFlags.PAUSED
    }
}
