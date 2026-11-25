package khaled.benzo.core.browser

data class BrowserTab(val id: Int, val url: String, val title: String)

/**
 * The browser's open tabs as an immutable value. There is always at least one tab; closing the last one leaves a blank tab.
 * Pure logic: the WebView, page state and saving live elsewhere.
 */
data class TabList(
    val tabs: List<BrowserTab>,
    val selectedId: Int,
    private val nextId: Int
) {
    val selected: BrowserTab get() = tabs.first { it.id == selectedId }

    /** Opens a tab and selects it. At [MAX_TABS] the new tab is not opened and the list is returned unchanged. */
    fun open(url: String = ""): TabList {
        if (tabs.size >= MAX_TABS) return this
        val t = BrowserTab(nextId, url, "")
        return copy(tabs = tabs + t, selectedId = t.id, nextId = nextId + 1)
    }

    fun select(id: Int): TabList = if (tabs.any { it.id == id }) copy(selectedId = id) else this

    /** Closes a tab. If it was selected, the tab that now sits at its position (or the last one) is selected. */
    fun close(id: Int): TabList {
        val idx = tabs.indexOfFirst { it.id == id }
        if (idx < 0) return this
        val rest = tabs.filterIndexed { i, _ -> i != idx }
        if (rest.isEmpty()) {
            val blank = BrowserTab(nextId, "", "")
            return TabList(listOf(blank), blank.id, nextId + 1)
        }
        val newSelected = if (id != selectedId) selectedId else rest[idx.coerceAtMost(rest.size - 1)].id
        return copy(tabs = rest, selectedId = newSelected)
    }

    fun update(id: Int, url: String?, title: String?): TabList =
        copy(tabs = tabs.map { if (it.id == id) it.copy(url = url ?: it.url, title = title ?: it.title) else it })

    /** One line per tab, "url TAB title", selected index on the first line. Only web addresses are kept. */
    fun serialize(): String {
        val web = tabs.filter { isWeb(it.url) }
        if (web.isEmpty()) return ""
        val selIdx = web.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
        return (listOf(selIdx.toString()) + web.map { "${clean(it.url)}\t${clean(it.title)}" }).joinToString("\n")
    }

    companion object {
        const val MAX_TABS = 20

        fun single(url: String = ""): TabList = TabList(listOf(BrowserTab(1, url, "")), 1, 2)

        /** Reads [serialize] output. Anything unreadable gives a fresh single tab. */
        fun parse(text: String?, fallbackUrl: String = ""): TabList {
            if (text.isNullOrBlank()) return single(fallbackUrl)
            val lines = text.lines()
            val selIdx = lines.firstOrNull()?.trim()?.toIntOrNull() ?: return single(fallbackUrl)
            val tabs = lines.drop(1).mapNotNull { l ->
                val parts = l.split('\t', limit = 2)
                val url = parts[0].trim()
                if (!isWeb(url)) null else url to (parts.getOrNull(1)?.trim() ?: "")
            }.take(MAX_TABS)
            if (tabs.isEmpty()) return single(fallbackUrl)
            val list = tabs.mapIndexed { i, (u, t) -> BrowserTab(i + 1, u, t) }
            return TabList(list, list[selIdx.coerceIn(0, list.size - 1)].id, list.size + 1)
        }

        private fun isWeb(url: String): Boolean {
            val l = url.lowercase()
            return l.startsWith("http://") || l.startsWith("https://")
        }

        private fun clean(s: String) = s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ')
    }
}
