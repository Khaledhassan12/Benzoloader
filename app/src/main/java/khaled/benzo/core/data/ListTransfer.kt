package khaled.benzo.core.data

import khaled.benzo.data.model.DownloadItem

/** One entry read from an imported list. */
data class ImportEntry(val url: String, val fileName: String?)

/**
 * Plain-text list format: one download per line, `URL` or `URL<TAB>file name`.
 * Lines that are empty or start with `#` are ignored.
 * Cookies, headers, referer and user-agent are never written. URLs are written unchanged: they may carry tokens or
 * user:password@host, so the file is NOT secret-free and says so in its header.
 */
object ListTransfer {
    const val HEADER = "# Benzoloader download list\n" +
        "# Cookies and headers are not included. Links are saved exactly as they are,\n" +
        "# so signed or private links (tokens in the query, user:password@host) still work and stay sensitive. Share with care."

    fun format(items: List<DownloadItem>): String {
        val sb = StringBuilder(HEADER).append('\n')
        for (i in items) {
            if (i.isTorrent) continue
            val name = i.fileName.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ')
            sb.append(i.url.trim()).append('\t').append(name).append('\n')
        }
        return sb.toString()
    }

    fun parse(text: String): List<ImportEntry> {
        val seen = LinkedHashSet<String>()
        val out = ArrayList<ImportEntry>()
        for (raw in text.lineSequence()) {
            val line = raw.trim().trimStart('\uFEFF')
            if (line.isEmpty() || line.startsWith("#")) continue
            val parts = line.split('\t', limit = 2)
            val url = parts[0].trim()
            val lower = url.lowercase()
            if (!(lower.startsWith("http://") || lower.startsWith("https://"))) continue
            if (url.length <= 8 || url.any { it.isWhitespace() }) continue
            if (!seen.add(url)) continue
            val name = parts.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
            out.add(ImportEntry(url, name))
        }
        return out
    }

    /** File name for an entry without one: last path segment, or "download". */
    fun nameFor(entry: ImportEntry): String {
        entry.fileName?.let { return it }
        val path = entry.url.substringAfter("://").substringBefore('?').substringBefore('#')
        val seg = path.substringAfter('/', "").trimEnd('/').substringAfterLast('/')
        if (seg.isEmpty()) return "download"
        // A stray "%" that is not an escape (for example "100%.zip") makes the decoder throw: keep the raw text then.
        return try { java.net.URLDecoder.decode(seg.replace("+", "%2B"), "UTF-8") } catch (e: IllegalArgumentException) { seg }
    }
}
