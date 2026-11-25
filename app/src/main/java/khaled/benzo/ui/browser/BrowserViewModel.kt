package khaled.benzo.ui.browser

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import khaled.benzo.core.browser.HistoryPolicy
import khaled.benzo.core.sniffer.DetectedMedia
import khaled.benzo.core.sniffer.MediaSniffer
import khaled.benzo.data.db.BenzoDatabase
import khaled.benzo.data.db.BookmarkEntity
import khaled.benzo.data.db.HistoryEntity
import khaled.benzo.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    /** Open tabs. One WebView shows the selected tab; the others wait as saved page states (kept while the screen lives, not on disk). */
    var tabs: khaled.benzo.core.browser.TabList = khaled.benzo.core.browser.TabList.single()
    var tabsStarted = false
    val tabStates = HashMap<Int, android.os.Bundle>()

    private val _detectedMediaList = MutableStateFlow<List<DetectedMedia>>(emptyList())
    val detectedMediaList: StateFlow<List<DetectedMedia>> = _detectedMediaList.asStateFlow()

    private val db = BenzoDatabase.getInstance(application)
    private val settings = SettingsRepository(application)

    val mediaSniffer = MediaSniffer { media: DetectedMedia ->
        val current = _detectedMediaList.value.toMutableList()
        if (current.none { it.url == media.url }) {
            current.add(media)
            _detectedMediaList.value = current
        }
    }

    fun clearDetectedMedia() {
        _detectedMediaList.value = emptyList()
    }

    val saveTabsEnabled: Boolean get() = settings.browserSaveTabs
    fun savedTabsText(): String? = settings.browserSavedTabs
    fun persistTabs() { if (settings.browserSaveTabs) settings.browserSavedTabs = tabs.serialize() }

    val historyEnabled: Boolean get() = settings.browserHistory

    /** Records a finished page load when the setting is on. Call from a background coroutine. */
    suspend fun recordVisit(title: String?, url: String?) {
        val dao = db.historyDao()
        if (!HistoryPolicy.shouldRecord(settings.browserHistory, url, dao.lastUrl())) return
        dao.insert(HistoryEntity(title = HistoryPolicy.label(title, url!!), url = url))
        dao.trimTo(HistoryPolicy.MAX_ROWS)
    }

    suspend fun history(): List<HistoryEntity> = db.historyDao().recent(HistoryPolicy.MAX_ROWS)

    suspend fun clearHistory() { db.historyDao().clear() }

    suspend fun bookmarks(): List<BookmarkEntity> = db.bookmarkDao().getAllOnce()

    /** Returns true when saved, false when the page cannot be bookmarked or already is. */
    suspend fun addBookmark(title: String?, url: String?): Boolean {
        val dao = db.bookmarkDao()
        if (!HistoryPolicy.canBookmark(url, url != null && dao.countByUrl(url) > 0)) return false
        dao.insertBookmark(BookmarkEntity(title = HistoryPolicy.label(title, url!!), url = url))
        return true
    }

    suspend fun deleteBookmark(id: Long) { db.bookmarkDao().deleteById(id) }
}
