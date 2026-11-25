package khaled.benzo.core.data

/** One bookmark or history row in transfer form. [time] is epoch millis. */
data class PageEntry(val url: String, val title: String, val time: Long)

/**
 * Text format for bookmarks and visit history: `URL<TAB>title<TAB>time`, header lines start with `#`.
 * Pure logic (no Android, no Room). Only http and https links are written and read.
 * Links are written as they are and may carry tokens, so the header says to share with care.
 */
object PageListTransfer {
    const val BOOKMARKS_HEADER = "# Benzoloader bookmarks\n# Links are saved exactly as they are. Share with care."
    const val HISTORY_HEADER = "# Benzoloader visit history\n# Links are saved exactly as they are. Share with care."

    private fun clean(s: String) = s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ').trim()
    private fun isWeb(u: String): Boolean {
        val l = u.lowercase()
        return (l.startsWith("http://") || l.startsWith("https://")) && u.length > 8 && u.none { it.isWhitespace() }
    }

    fun format(header: String, entries: List<PageEntry>): String {
        val sb = StringBuilder(header).append('\n')
        for (e in entries) {
            val url = e.url.trim()
            if (!isWeb(url)) continue
            sb.append(url).append('\t').append(clean(e.title)).append('\t').append(e.time).append('\n')
        }
        return sb.toString()
    }

    fun parse(text: String): List<PageEntry> {
        val seen = HashSet<String>()
        val out = ArrayList<PageEntry>()
        for (raw in text.lineSequence()) {
            val line = raw.trim().trimStart('\uFEFF')
            if (line.isEmpty() || line.startsWith("#")) continue
            val p = line.split('\t')
            val url = p[0].trim()
            if (!isWeb(url) || !seen.add(url)) continue
            out.add(PageEntry(url, p.getOrNull(1)?.trim().orEmpty(), p.getOrNull(2)?.trim()?.toLongOrNull()?.takeIf { it >= 0 } ?: 0L))
        }
        return out
    }
}
