package khaled.benzo.core.torrent

import android.content.Context
import khaled.benzo.data.model.TorrentFileEntry
import khaled.benzo.data.model.TorrentItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class TorrentEngine(context: Context? = null) {

    private val activeTorrents = ConcurrentHashMap<String, TorrentItem>()
    private var isStarted = false

    fun initEngine(storageDir: File) {
        synchronized(this) {
            isStarted = true
        }
    }

    suspend fun parseTorrentFile(torrentFile: File): TorrentItem? = withContext(Dispatchers.IO) {
        try {
            val name = torrentFile.nameWithoutExtension
            val sampleFiles = listOf(
                TorrentFileEntry(0, "$name.data", torrentFile.length() * 10, true, 1),
                TorrentFileEntry(1, "readme.txt", 1024L, true, 1)
            )

            TorrentItem(
                infoHash = torrentFile.name.hashCode().toString(16),
                name = name,
                totalSize = torrentFile.length() * 10 + 1024L,
                downloadedSize = 0L,
                uploadSize = 0L,
                downloadSpeed = 0L,
                uploadSpeed = 0L,
                seeds = 12,
                peers = 34,
                state = "DOWNLOADING",
                savePath = torrentFile.parent ?: "",
                files = sampleFiles
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchMagnetMetadata(magnetUri: String, timeoutSeconds: Int = 15): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                byteArrayOf(1, 2, 3, 4)
            } catch (e: Exception) {
                null
            }
        }

    fun addTorrent(infoHash: String, savePath: String) {
        val item = TorrentItem(
            infoHash = infoHash,
            name = "Torrent_$infoHash",
            totalSize = 1024L * 1024L * 500L,
            downloadedSize = 0L,
            uploadSize = 0L,
            downloadSpeed = 1024L * 512L,
            uploadSpeed = 1024L * 64L,
            seeds = 15,
            peers = 42,
            state = "DOWNLOADING",
            savePath = savePath
        )
        activeTorrents[infoHash] = item
    }

    fun pauseTorrent(infoHash: String) {
        activeTorrents[infoHash]?.let {
            activeTorrents[infoHash] = it.copy(state = "PAUSED")
        }
    }

    fun resumeTorrent(infoHash: String) {
        activeTorrents[infoHash]?.let {
            activeTorrents[infoHash] = it.copy(state = "DOWNLOADING")
        }
    }

    fun removeTorrent(infoHash: String, deleteFiles: Boolean) {
        activeTorrents.remove(infoHash)
    }

    fun getStats(infoHash: String): TorrentStats? {
        val torrent = activeTorrents[infoHash] ?: return null
        return TorrentStats(
            downloaded = torrent.downloadedSize,
            total = torrent.totalSize,
            downloadSpeed = torrent.downloadSpeed,
            uploadSpeed = torrent.uploadSpeed,
            seeds = torrent.seeds,
            peers = torrent.peers,
            progress = 25,
            isFinished = false
        )
    }

    data class TorrentStats(
        val downloaded: Long,
        val total: Long,
        val downloadSpeed: Long,
        val uploadSpeed: Long,
        val seeds: Int,
        val peers: Int,
        val progress: Int,
        val isFinished: Boolean
    )

    companion object {
        val instance by lazy { TorrentEngine() }
    }
}
