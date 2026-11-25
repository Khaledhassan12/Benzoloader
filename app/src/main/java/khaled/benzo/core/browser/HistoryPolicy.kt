package khaled.benzo.core.browser

/** Pure rules for browser history and bookmarks. */
object HistoryPolicy {
    const val MAX_ROWS = 500
    const val MAX_URL_LENGTH = 2048

    /** Only real web pages are recorded, never twice in a row, and never when the setting is off. */
    fun shouldRecord(enabled: Boolean, url: String?, lastUrl: String?): Boolean {
        if (!enabled || url.isNullOrBlank()) return false
        val lower = url.lowercase()
        if (!(lower.startsWith("http://") || lower.startsWith("https://"))) return false
        if (url.length > MAX_URL_LENGTH) return false
        return url != lastUrl
    }

    /** What the list shows for an entry: the page title, or the address when there is no title. */
    fun label(title: String?, url: String): String = title?.trim()?.takeIf { it.isNotEmpty() } ?: url

    /** A bookmark needs a web address and must not already exist. */
    fun canBookmark(url: String?, alreadySaved: Boolean): Boolean {
        if (url.isNullOrBlank() || alreadySaved) return false
        val lower = url.lowercase()
        return lower.startsWith("http://") || lower.startsWith("https://")
    }
}
