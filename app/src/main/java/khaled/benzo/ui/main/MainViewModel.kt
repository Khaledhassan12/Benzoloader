package khaled.benzo.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import khaled.benzo.data.model.DownloadItem
import khaled.benzo.data.model.FileCategory
import khaled.benzo.data.repository.DownloadRepository
import khaled.benzo.core.queue.ListState
import khaled.benzo.core.queue.SortKey
import khaled.benzo.core.queue.StatusFilter
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DownloadRepository(application)
    private val _state = MutableStateFlow(ListState(ascending = khaled.benzo.data.repository.SettingsRepository(application).listAddToBottom))
    private val _selectedCategory = MutableStateFlow<FileCategory?>(FileCategory.ALL)

    val downloads: StateFlow<List<DownloadItem>> = MutableStateFlow<List<DownloadItem>>(emptyList())

    /** Counts for the bottom bar badges: (unfinished, finished), over all downloads regardless of the current filter. */
    val counts: StateFlow<Pair<Int, Int>> = MutableStateFlow(0 to 0)

    val selectedCategory = _selectedCategory.asStateFlow()
    val listState = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllDownloads(null).collect { all ->
                val done = all.count { it.status == khaled.benzo.data.model.DownloadStatus.COMPLETED }
                (counts as MutableStateFlow).value = (all.size - done) to done
            }
        }
        viewModelScope.launch {
            combine(repository.getAllDownloads(null), _state) { all, st -> st.apply(all) }.collect { list ->
                (downloads as MutableStateFlow).value = list
            }
        }
    }

    /** Drawer entries: ALL and the status entries set a status filter, the others filter by file type. */
    fun setCategory(category: FileCategory?) {
        _selectedCategory.value = category
        _state.update { it.withCategory(category) }
    }

    fun setStatusFilter(filter: StatusFilter) {
        _selectedCategory.value = null
        _state.update { it.withStatus(filter) }
    }

    fun setQuery(query: String) = _state.update { it.withQuery(query) }

    fun setSort(key: SortKey) = _state.update { it.withSort(key) }

    fun deleteDownload(id: Long) {
        viewModelScope.launch {
            repository.deleteDownload(id)
        }
    }

    fun pauseAll() {
        viewModelScope.launch {
            repository.pauseAll()
        }
    }

    fun resumeAll() {
        viewModelScope.launch {
            repository.resumeAll()
        }
    }

    /** Text of the current list (what the user sees, after filters). Contains no cookies or headers. */
    fun exportText(): String = khaled.benzo.core.data.ListTransfer.format(downloads.value)

    /** Adds imported URLs as paused downloads (the user starts them). */
    fun importText(text: String, onDone: (added: Int, skipped: Int) -> Unit) {
        viewModelScope.launch {
            val r = khaled.benzo.core.data.ListImporter(getApplication(), repository).importText(text)
            onDone(r.added, r.skipped)
        }
    }

    /** Saves re-entered credentials (or drops the unreadable ones) without touching progress. */
    fun repairCredentials(id: Long, cookies: String?, headers: String?, discard: Boolean, onError: () -> Unit) {
        viewModelScope.launch {
            val item = repository.getDownload(id) ?: return@launch
            val fixed = khaled.benzo.core.security.CredentialRepair.apply(item, cookies, headers, discard)
            if (fixed == item) return@launch
            runCatching { repository.updateDownload(fixed) }.onFailure { onError() }
        }
    }
}
