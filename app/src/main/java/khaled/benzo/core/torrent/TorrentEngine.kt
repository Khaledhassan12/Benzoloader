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
            if (!session.isRunning) {
                session.start()
                session.addListener(resumeListener)
                libraryConnectionsDefault = session.settings().connectionsLimit()
                libraryListenDefault = session.settings().listenInterfaces()
                applyNetwork()
            }
        }
    }

    /** Receives (infoHash, resume bytes) whenever libtorrent hands over resume data; the caller stores it. Set once by the task runner. */
    @Volatile var resumeSink: ((String, ByteArray) -> Unit)? = null

    private val resumeListener = object : com.frostwire.jlibtorrent.AlertListener {
        override fun types(): IntArray = intArrayOf(com.frostwire.jlibtorrent.alerts.AlertType.SAVE_RESUME_DATA.swig())
        override fun alert(alert: com.frostwire.jlibtorrent.alerts.Alert<*>) {
            val a = alert as? com.frostwire.jlibtorrent.alerts.SaveResumeDataAlert ?: return
            try {
                val buf = com.frostwire.jlibtorrent.swig.add_torrent_params.write_resume_data_buf(a.params().swig())
                resumeSink?.invoke(a.handle().infoHash().toHex(), com.frostwire.jlibtorrent.Vectors.byte_vector2bytes(buf))
            } catch (e: Exception) {
                // Resume data is only a speed-up; losing one snapshot is not an error.
            }
        }
    }

    /** Asks libtorrent for fresh resume data of one torrent; it arrives later through [resumeSink]. Does nothing when nothing changed. */
    fun requestResumeData(infoHash: String) {
        val h = handle(infoHash)?.takeIf { it.isValid } ?: return
        if (!h.needSaveResumeData()) return
        h.saveResumeData(com.frostwire.jlibtorrent.TorrentHandle.FLUSH_DISK_CACHE.or_(com.frostwire.jlibtorrent.TorrentHandle.SAVE_INFO_DICT))
    }

    /** Adds a torrent from its saved resume data. Returns false (and adds nothing) when the data cannot be read. */
    private fun addWithResume(resume: ByteArray, saveDir: File, ti: TorrentInfo?): Boolean {
        val ec = com.frostwire.jlibtorrent.swig.error_code()
        val params = com.frostwire.jlibtorrent.swig.add_torrent_params.read_resume_data(com.frostwire.jlibtorrent.Vectors.bytes2byte_vector(resume), ec)
        if (ec.value() != 0) return false
        params.setSave_path(saveDir.absolutePath)
        if (ti != null) params.set_ti(ti.swig())
        // A torrent saved while paused must still start when the user starts it.
        params.setFlags(params.getFlags().and_(PAUSED_FLAG.inv()))
        session.swig().async_add_torrent(params)
        return true
    }

    private var libraryConnectionsDefault = 0
    private var wantDht = true
    private var wantConnections = 0
    private var wantPort = 0
    private var wantAnonymous = false
    private var libraryListenDefault = ""
    private var wantEncryption = 0

    /** Network settings. Applied at once when the session runs, otherwise right after it starts. Cheap to call repeatedly. */
    fun configureNetwork(dht: Boolean, connectionsLimit: Int, fixedPort: Int, anonymous: Boolean, encryption: Int = 0) = synchronized(lock) {
        if (dht == wantDht && connectionsLimit == wantConnections && fixedPort == wantPort && anonymous == wantAnonymous && encryption == wantEncryption) return@synchronized
        wantEncryption = encryption
        wantDht = dht; wantConnections = connectionsLimit; wantPort = fixedPort; wantAnonymous = anonymous
        if (session.isRunning) applyNetwork()
    }

    private var wantLsd = true
    private var wantUtp = true
    private var wantUpnp = true
    private var wantNatpmp = true

    /** Local discovery, uTP, UPnP and NAT-PMP switches. Cheap to call repeatedly. */
    fun configureDiscovery(lsd: Boolean, utp: Boolean, upnp: Boolean, natpmp: Boolean) = synchronized(lock) {
        if (lsd == wantLsd && utp == wantUtp && upnp == wantUpnp && natpmp == wantNatpmp) return@synchronized
        wantLsd = lsd; wantUtp = utp; wantUpnp = upnp; wantNatpmp = natpmp
        if (session.isRunning) applyNetwork()
    }

    private fun applyNetwork() {
        val pack = session.settings()
        pack.enableDht(wantDht)
        pack.connectionsLimit(TorrentNetworkPolicy.connectionsToApply(wantConnections, libraryConnectionsDefault))
        pack.anonymousMode(wantAnonymous)
        pack.setBoolean(com.frostwire.jlibtorrent.swig.settings_pack.bool_types.enable_lsd.swigValue(), wantLsd)
        pack.setBoolean(com.frostwire.jlibtorrent.swig.settings_pack.bool_types.enable_upnp.swigValue(), wantUpnp)
        pack.setBoolean(com.frostwire.jlibtorrent.swig.settings_pack.bool_types.enable_natpmp.swigValue(), wantNatpmp)
        pack.setBoolean(com.frostwire.jlibtorrent.swig.settings_pack.bool_types.enable_outgoing_utp.swigValue(), wantUtp)
        pack.setBoolean(com.frostwire.jlibtorrent.swig.settings_pack.bool_types.enable_incoming_utp.swigValue(), wantUtp)
        // Policy for both directions. API names read from the jlibtorrent 1.2.0.18 classes, NOT compiled yet.
        val policy = when (TorrentEncryption.fromIndex(wantEncryption)) {
            TorrentEncryption.REQUIRE -> com.frostwire.jlibtorrent.swig.settings_pack.enc_policy.pe_forced
            TorrentEncryption.DISABLE -> com.frostwire.jlibtorrent.swig.settings_pack.enc_policy.pe_disabled
            TorrentEncryption.PREFER -> com.frostwire.jlibtorrent.swig.settings_pack.enc_policy.pe_enabled
        }.swigValue()
        pack.setInteger(com.frostwire.jlibtorrent.swig.settings_pack.int_types.out_enc_policy.swigValue(), policy)
        pack.setInteger(com.frostwire.jlibtorrent.swig.settings_pack.int_types.in_enc_policy.swigValue(), policy)
        if (wantPort != 0) pack.listenInterfaces(TorrentNetworkPolicy.listenInterfaces(wantPort))
        else if (libraryListenDefault.isNotEmpty()) pack.listenInterfaces(libraryListenDefault)
        session.applySettings(pack)
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
    fun addTorrentBytes(torrentBytes: ByteArray, saveDir: File, priorities: List<Int>? = null, resume: ByteArray? = null): String {
        ensureStarted()
        saveDir.mkdirs()
        val ti = TorrentInfo.bdecode(torrentBytes)
        if (resume != null && addWithResume(resume, saveDir, ti)) return ti.infoHash().toHex()
        val prios = priorities?.let { list ->
            Array(ti.numFiles()) { i -> toPriority(list.getOrElse(i) { 1 }) }
        }
        session.download(ti, saveDir, null, prios, null)
        return ti.infoHash().toHex()
    }

    fun addTorrentFile(torrentFile: File, saveDir: File, priorities: List<Int>? = null, resume: ByteArray? = null): String =
        addTorrentBytes(torrentFile.readBytes(), saveDir, priorities, resume)

    /** Adds a magnet link; metadata is fetched by the session after adding. Returns the info hash. */
    fun addMagnet(magnetUri: String, saveDir: File, resume: ByteArray? = null): String? {
        val hash = TorrentMapping.magnetInfoHash(magnetUri) ?: return null
        ensureStarted()
        saveDir.mkdirs()
        if (resume != null && addWithResume(resume, saveDir, null)) return hash
        session.download(magnetUri, saveDir)
        return hash
    }

    private fun handle(infoHash: String) = session.find(Sha1Hash(infoHash))

    fun hasTorrent(infoHash: String): Boolean = session.isRunning && handle(infoHash)?.isValid == true

    /** Turns sequential download on or off for a torrent the session knows. Returns false when the session has no such torrent yet. */
    fun setSequential(infoHash: String, sequential: Boolean): Boolean {
        val h = handle(infoHash)?.takeIf { it.isValid } ?: return false
        if (sequential) h.setFlags(TORRENT_SEQUENTIAL) else h.unsetFlags(TORRENT_SEQUENTIAL)
        return true
    }

    /** Tracker addresses of a torrent in the live session, or null when it is not there. */
    fun trackers(infoHash: String): List<String>? {
        val h = handle(infoHash)?.takeIf { it.isValid } ?: return null
        return h.trackers().map { it.url() }
    }

    /**
     * Makes the torrent's tracker list exactly [urls] (already checked by TrackerPolicy), keeping the order as tiers.
     * Lives in the running session only: the stored torrent data is not changed, so it is not remembered after a restart.
     */
    fun replaceTrackers(infoHash: String, urls: List<String>): Boolean {
        val h = handle(infoHash)?.takeIf { it.isValid } ?: return false
        h.replaceTrackers(urls.mapIndexed { i, u -> com.frostwire.jlibtorrent.AnnounceEntry(u).also { it.tier(i.toShort()) } })
        return true
    }

    /** Re-checks the files on disk against the torrent (jlibtorrent TorrentHandle.forceRecheck, descriptor ()V read from the class). False when the torrent is not loaded. */
    fun forceRecheck(infoHash: String): Boolean {
        val h = handle(infoHash)?.takeIf { it.isValid } ?: return false
        h.forceRecheck(); return true
    }

    /** Asks every tracker for peers now (TorrentHandle.forceReannounce, descriptor ()V read from the class). False when the torrent is not loaded. */
    fun forceReannounce(infoHash: String): Boolean {
        val h = handle(infoHash)?.takeIf { it.isValid } ?: return false
        h.forceReannounce(); return true
    }

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

    /** The .torrent bytes once the session has the metadata (a magnet link gets it from the swarm), else null. */
    fun metadataBytes(infoHash: String): ByteArray? {
        val h = handle(infoHash)?.takeIf { it.isValid } ?: return null
        if (!h.status().hasMetadata()) return null
        return try { h.torrentFile()?.takeIf { it.isValid }?.bencode() } catch (e: Exception) { null }
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
            uploaded = st.allTimeUpload(),
            seedingSeconds = st.seedingDuration(),
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
        val state: String = "UNKNOWN",
        val uploaded: Long = 0L,
        val seedingSeconds: Long = 0L
    )

    companion object {
        private val capability = TorrentCapability { TorrentEngine() }
        /** The engine, or null when the native torrent library cannot load on this device. */
        fun orNull(): TorrentEngine? = capability.engineOrNull()
        /** Why torrents are unavailable (null when they work). Shown instead of crashing. */
        fun unavailableReason(): String? { capability.engineOrNull(); return capability.diagnostic() }
        private val SESSION_DELETE_FILES: remove_flags_t = com.frostwire.jlibtorrent.SessionHandle.DELETE_FILES
        private val TORRENT_SEQUENTIAL = com.frostwire.jlibtorrent.TorrentFlags.SEQUENTIAL_DOWNLOAD
        private val PAUSED_FLAG = com.frostwire.jlibtorrent.TorrentFlags.PAUSED
    }
}
