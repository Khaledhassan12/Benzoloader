package khaled.benzo.ui.torrent

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import khaled.benzo.core.torrent.TorrentEngine
import khaled.benzo.core.torrent.TorrentMapping
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

    private fun readBytes(s: String): ByteArray? {
        val ctx = getApplication<Application>()
        val uri = Uri.parse(s)
        return when (uri.scheme) {
            "content" -> ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            "file" -> uri.path?.let { File(it).readBytes() }
            null -> File(s).takeIf { it.exists() }?.readBytes()
            else -> null
        }
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
                    val prios = TorrentMapping.effectivePriorities(files.map { it.isSelected }, files.map { it.priority })
                    val bytes = torrentBytes
                    if (bytes != null) torrentEngine.addTorrentBytes(bytes, File(savePath), prios)
                    else if (TorrentMapping.isMagnet(source)) torrentEngine.addMagnet(source, File(savePath))
                    else null
                } catch (e: Exception) {
                    null
                }
            }
            if (hash == null) _error.value = "Could not start torrent"
            onResult(hash)
        }
    }
}
