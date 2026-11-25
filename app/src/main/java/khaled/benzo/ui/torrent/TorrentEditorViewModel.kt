package khaled.benzo.ui.torrent

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import khaled.benzo.core.torrent.BencodeValidator
import khaled.benzo.core.torrent.TorrentEngine
import khaled.benzo.core.torrent.TorrentMapping
import khaled.benzo.core.torrent.TorrentSourcePolicy
import khaled.benzo.data.model.TorrentFileEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class TorrentEditorViewModel(application: Application) : AndroidViewModel(application) {

    val torrentEngine = TorrentEngine.instance

    private val _torrentFiles = MutableStateFlow<List<TorrentFileEntry>>(emptyList())
    val torrentFiles: StateFlow<List<TorrentFileEntry>> = _torrentFiles.asStateFlow()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var source: String = ""
    private var torrentBytes: ByteArray? = null

    /** Loads real metadata from a .torrent file, a content/file URI, or a magnet link. */
    fun loadTorrent(filePathOrUri: String) {
        source = filePathOrUri
        viewModelScope.launch {
            _error.value = null
            val item = withContext(Dispatchers.IO) {
                try {
                    if (TorrentMapping.isMagnet(filePathOrUri)) {
                        val bytes = torrentEngine.fetchMagnetMetadata(filePathOrUri, 30)
                        torrentBytes = bytes
                        bytes?.let { torrentEngine.toItem(com.frostwire.jlibtorrent.TorrentInfo.bdecode(it), "") }
                    } else {
                        val bytes = readBytes(filePathOrUri)
                        torrentBytes = bytes
                        bytes?.let { torrentEngine.toItem(com.frostwire.jlibtorrent.TorrentInfo.bdecode(it), "") }
                    }
                } catch (e: Exception) {
                    null
                }
            }
            if (item == null) {
                _error.value = "Could not read torrent metadata"
                _torrentFiles.value = emptyList()
            } else {
                _name.value = item.name
                _torrentFiles.value = item.files
            }
        }
    }

    /** Reads a .torrent from another app's intent. See [TorrentSourcePolicy] for what is allowed and why. */
    private fun readBytes(s: String): ByteArray? {
        val ctx = getApplication<Application>()
        val bytes = when (TorrentSourcePolicy.classify(s)) {
            TorrentSourcePolicy.Kind.CONTENT -> {
                val uri = Uri.parse(s.trim())
                // A provider of this very app (for example the file provider) must not be read on behalf of another app.
                val owner = uri.authority?.let { ctx.packageManager.resolveContentProvider(it, 0)?.packageName }
                if (owner == ctx.packageName) null
                else ctx.contentResolver.openInputStream(uri)?.use { TorrentSourcePolicy.readLimited(it) }
            }
            TorrentSourcePolicy.Kind.FILE -> {
                val raw = s.trim()
                val path = if (raw.startsWith("/")) raw else Uri.parse(raw).path
                val file = path?.let { File(it) }?.takeIf { it.isFile }
                val canonical = file?.canonicalPath
                if (canonical != null && fileAllowed(ctx, canonical)) file.inputStream().use { TorrentSourcePolicy.readLimited(it) } else null
            }
            else -> null
        }
        return bytes?.takeIf { BencodeValidator.validate(it) is BencodeValidator.Result.Ok }
    }

    private fun fileAllowed(ctx: Application, canonical: String): Boolean {
        val shared = android.os.Environment.getExternalStorageDirectory().canonicalPath
        val denied = listOfNotNull(
            ctx.dataDir.canonicalPath,
            ctx.getExternalFilesDir(null)?.parentFile?.canonicalPath // .../Android/data/<package>
        )
        return TorrentSourcePolicy.isFileAllowed(canonical, listOf(shared), denied)
    }

    fun setSelected(index: Int, selected: Boolean) {
        _torrentFiles.value = _torrentFiles.value.map { if (it.index == index) it.copy(isSelected = selected) else it }
    }

    /** Starts the real download with the chosen files. Returns the info hash or null on failure. */
    fun startDownload(savePath: String, onResult: (String?) -> Unit = {}) {
        viewModelScope.launch {
            val hash = withContext(Dispatchers.IO) {
                try {
                    val files = _torrentFiles.value
                    val settings = khaled.benzo.data.repository.SettingsRepository(getApplication())
                    val rootPath = settings.defaultDownloadPath.ifEmpty { savePath }
                    val savePath = khaled.benzo.core.catalog.CatalogPolicy.targetFolder(
                        savePath, rootPath, khaled.benzo.data.model.FileCategory.TORRENT, settings.catalogEnabled, settings.catalogFolderNames)
                    val prios = TorrentMapping.effectivePriorities(files.map { it.isSelected }, files.map { it.priority })
                    val bytes = torrentBytes
                    val ctx = getApplication<Application>()
                    val hash: String?
                    var url = source
                    if (bytes != null) {
                        hash = com.frostwire.jlibtorrent.TorrentInfo.bdecode(bytes).infoHash().toHex()
                        val dir = File(ctx.filesDir, "torrents").apply { mkdirs() }
                        File(dir, "$hash.torrent").writeBytes(bytes)
                        File(dir, "$hash.prio").writeText(TorrentMapping.encodePriorities(prios))
                        url = File(dir, "$hash.torrent").absolutePath
                    } else {
                        hash = TorrentMapping.magnetInfoHash(source)
                    }
                    if (hash != null) {
                        val wanted = files.filter { it.isSelected }.sumOf { it.size }
                        khaled.benzo.data.repository.DownloadRepository(ctx).insertDownload(
                            khaled.benzo.data.model.DownloadItem(
                                url = url,
                                fileName = _name.value.ifBlank { hash },
                                savePath = savePath,
                                totalBytes = if (files.isEmpty()) -1L else wanted,
                                category = khaled.benzo.data.model.FileCategory.TORRENT,
                                isTorrent = true,
                                torrentInfoHash = hash
                            )
                        )
                        ctx.startService(android.content.Intent(ctx, khaled.benzo.service.DownloadService::class.java)
                            .setAction(khaled.benzo.service.DownloadService.ACTION_PROCESS_QUEUE))
                    }
                    hash
                } catch (e: Exception) {
                    null
                }
            }
            if (hash == null) _error.value = "Could not start torrent"
            onResult(hash)
        }
    }
}
