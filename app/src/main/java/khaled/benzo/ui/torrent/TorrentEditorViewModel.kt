package khaled.benzo.ui.torrent

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import khaled.benzo.core.torrent.TorrentEngine
import khaled.benzo.data.model.TorrentFileEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TorrentEditorViewModel(application: Application) : AndroidViewModel(application) {

    val torrentEngine = TorrentEngine(application)

    private val _torrentFiles = MutableStateFlow<List<TorrentFileEntry>>(emptyList())
    val torrentFiles: StateFlow<List<TorrentFileEntry>> = _torrentFiles.asStateFlow()

    fun loadTorrent(filePathOrUri: String) {
        viewModelScope.launch {
            // Load or parse torrent files
            val sampleFiles = listOf(
                TorrentFileEntry(0, "content_main.iso", 1024L * 1024L * 850L, true, 1),
                TorrentFileEntry(1, "readme.txt", 1024L * 12L, true, 1)
            )
            _torrentFiles.value = sampleFiles
        }
    }

    fun startDownload(infoHash: String, savePath: String) {
        viewModelScope.launch {
            torrentEngine.addTorrent(infoHash, savePath)
        }
    }
}
