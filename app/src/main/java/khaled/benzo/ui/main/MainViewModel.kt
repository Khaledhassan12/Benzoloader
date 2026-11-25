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
    private val _state = MutableStateFlow(khaled.benzo.data.repository.SettingsRepository(application).listAddToBottom.let { ListState(ascending = it, addToBottom = it) })
    private val _selectedCategory = MutableStateFlow<FileCategory?>(FileCategory.ALL)

    val downloads: StateFlow<List<DownloadItem>> = MutableStateFlow<List<DownloadItem>>(emptyList())

    /** Every download, unfiltered: the side panel counts come from this. */
    val allItems: StateFlow<List<DownloadItem>> = MutableStateFlow<List<DownloadItem>>(emptyList())

    /** Counts for the bottom bar badges: (unfinished, finished), over all downloads regardless of the current filter. */
    val counts: StateFlow<Pair<Int, Int>> = MutableStateFlow(0 to 0)

    val selectedCategory = _selectedCategory.asStateFlow()
    val listState = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllDownloads(null).collect { all ->
                val done = all.count { it.status == khaled.benzo.data.model.DownloadStatus.COMPLETED }
                (counts as MutableStateFlow).value = (all.size - done) to done
                (allItems as MutableStateFlow).value = all
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

    /** Re-reads the stored display-order setting; called when the screen comes back from Settings. */
    fun setAddToBottom(bottom: Boolean) = _state.update { it.withAddToBottom(bottom) }

    fun setStatusOnly(f: StatusFilter) = _state.update { it.withStatusOnly(f) }
    fun setTypeOnly(c: FileCategory?) = _state.update { it.withTypeOnly(c) }
    fun setTime(f: khaled.benzo.core.queue.TimeFilter) = _state.update { it.withTime(f) }
    fun setSortDirection(key: SortKey, ascending: Boolean) = _state.update { it.withSortDirection(key, ascending) }
    fun clearFilters() = _state.update { it.cleared() }

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

    private fun db() = khaled.benzo.data.db.BenzoDatabase.getInstance(getApplication())

    /** Bookmarks as transfer text (read from the database; no cookies, no credentials). */
    suspend fun exportBookmarksText(): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        khaled.benzo.core.data.PageListTransfer.format(
            khaled.benzo.core.data.PageListTransfer.BOOKMARKS_HEADER,
            db().bookmarkDao().getAllOnce().map { khaled.benzo.core.data.PageEntry(it.url, it.title, it.createdAt) })
    }

    /** Visit history as transfer text. */
    suspend fun exportHistoryText(): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        khaled.benzo.core.data.PageListTransfer.format(
            khaled.benzo.core.data.PageListTransfer.HISTORY_HEADER,
            db().historyDao().recent(Int.MAX_VALUE).map { khaled.benzo.core.data.PageEntry(it.url, it.title, it.visitedAt) })
    }

    /** Adds bookmarks that are not saved yet (matched by URL). Returns (added, skipped). */
    suspend fun importBookmarksText(text: String): Pair<Int, Int> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val dao = db().bookmarkDao()
        var added = 0; var skipped = 0
        for (e in khaled.benzo.core.data.PageListTransfer.parse(text)) {
            if (dao.countByUrl(e.url) > 0) { skipped++; continue }
            dao.insertBookmark(khaled.benzo.data.db.BookmarkEntity(title = e.title.ifEmpty { e.url }, url = e.url, createdAt = if (e.time > 0) e.time else System.currentTimeMillis()))
            added++
        }
        added to skipped
    }

    /** Finished downloads as records (no cookies, no headers). */
    fun exportFinishedText(): String = khaled.benzo.core.data.FinishedTransfer.format(
        allItems.value.filter { !it.isTorrent && it.status == khaled.benzo.data.model.DownloadStatus.COMPLETED }
            .map { khaled.benzo.core.data.FinishedRecord(it.url, it.fileName, it.savePath, it.totalBytes, it.completedDate, it.category.key) })

    /** Restores finished records as COMPLETED entries. Never queues anything, never touches files. Returns (added, skipped). */
    suspend fun importFinishedText(text: String): Pair<Int, Int> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val existing = db().downloadDao().getAllOnce().map { Triple(it.url, it.savePath, it.fileName) }.toHashSet()
        var added = 0; var skipped = 0
        for (r in khaled.benzo.core.data.FinishedTransfer.parse(text)) {
            if (!existing.add(Triple(r.url, r.folder, r.fileName))) { skipped++; continue }
            repository.insertDownload(khaled.benzo.data.model.DownloadItem(
                url = r.url, fileName = r.fileName, savePath = r.folder, totalBytes = r.totalBytes,
                downloadedBytes = if (r.totalBytes > 0) r.totalBytes else 0L,
                status = khaled.benzo.data.model.DownloadStatus.COMPLETED, completedDate = r.completedAt,
                category = khaled.benzo.data.model.FileCategory.entries.firstOrNull { it.key == r.category } ?: khaled.benzo.data.model.FileCategory.OTHER))
            added++
        }
        added to skipped
    }

    /** Adds history rows that are not stored yet (same URL and time). Returns (added, skipped). */
    suspend fun importHistoryText(text: String): Pair<Int, Int> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val dao = db().historyDao()
        var added = 0; var skipped = 0
        for (e in khaled.benzo.core.data.PageListTransfer.parse(text)) {
            val at = if (e.time > 0) e.time else System.currentTimeMillis()
            if (dao.countExact(e.url, at) > 0) { skipped++; continue }
            dao.insert(khaled.benzo.data.db.HistoryEntity(title = e.title.ifEmpty { e.url }, url = e.url, visitedAt = at))
            added++
        }
        added to skipped
    }

    /** Same as [importText] but suspending, for bundle import. */
    suspend fun importDownloadsText(text: String): Pair<Int, Int> {
        val r = khaled.benzo.core.data.ListImporter(getApplication(), repository).importText(text)
        return r.added to r.skipped
    }

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
