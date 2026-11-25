package khaled.benzo.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import khaled.benzo.data.model.DownloadItem
import khaled.benzo.data.model.FileCategory
import khaled.benzo.data.repository.DownloadRepository
import khaled.benzo.core.queue.ListLogic
import khaled.benzo.core.queue.SortKey
import khaled.benzo.core.queue.StatusFilter
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Everything the list needs to decide what to show. */
data class ListState(
    val status: StatusFilter = StatusFilter.ALL,
    val type: FileCategory? = null,
    val query: String = "",
    val sort: SortKey = SortKey.TIME,
    val ascending: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DownloadRepository(application)
    private val _state = MutableStateFlow(ListState())
    private val _selectedCategory = MutableStateFlow<FileCategory?>(FileCategory.ALL)

    val downloads: StateFlow<List<DownloadItem>> = MutableStateFlow<List<DownloadItem>>(emptyList())

    val selectedCategory = _selectedCategory.asStateFlow()
    val listState = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repository.getAllDownloads(null), _state) { all, st ->
                ListLogic.apply(all, st.status, st.query, st.sort, st.ascending)
                    .let { list -> if (st.type == null) list else list.filter { it.category == st.type } }
            }.collect { list ->
                (downloads as MutableStateFlow).value = list
            }
        }
    }

    /** Drawer entries: ALL and the status entries set a status filter, the others filter by file type. */
    fun setCategory(category: FileCategory?) {
        _selectedCategory.value = category
        _state.update {
            when (category) {
                null, FileCategory.ALL -> it.copy(status = StatusFilter.ALL, type = null)
                FileCategory.DOWNLOADING -> it.copy(status = StatusFilter.ACTIVE, type = null)
                FileCategory.COMPLETED -> it.copy(status = StatusFilter.COMPLETED, type = null)
                FileCategory.QUEUED -> it.copy(status = StatusFilter.QUEUED, type = null)
                else -> it.copy(status = StatusFilter.ALL, type = category)
            }
        }
    }

    fun setStatusFilter(filter: StatusFilter) {
        _selectedCategory.value = null
        _state.update { it.copy(status = filter, type = null) }
    }

    fun setQuery(query: String) = _state.update { it.copy(query = query) }

    /** Picking the active sort key again flips the direction; a new key starts in its natural direction. */
    fun setSort(key: SortKey) = _state.update {
        if (it.sort == key) it.copy(ascending = !it.ascending)
        else it.copy(sort = key, ascending = key == SortKey.NAME || key == SortKey.CATEGORY)
    }

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
