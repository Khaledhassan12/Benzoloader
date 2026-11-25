package khaled.benzo.core.data

/** A finished download as a record (no file content). Times are epoch millis. */
data class FinishedRecord(
    val url: String, val fileName: String, val folder: String,
    val totalBytes: Long, val completedAt: Long, val category: String
)

/**
 * Round-trip text for the "finished" section: `URL<TAB>name<TAB>folder<TAB>bytes<TAB>completedAt<TAB>category`.
 * Restoring it recreates COMPLETED records only; it never queues anything and never touches files.
 * Cookies, headers and credentials are not written. Pure logic.
 */
object FinishedTransfer {
    private fun clean(s: String) = s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ').trim()
    private fun isWeb(u: String): Boolean {
        val l = u.lowercase()
        return (l.startsWith("http://") || l.startsWith("https://")) && u.length > 8 && u.none { it.isWhitespace() }
    }

    fun format(records: List<FinishedRecord>): String {
        val sb = StringBuilder("# Benzoloader finished downloads (records only)\n")
        for (r in records) {
            if (!isWeb(r.url.trim())) continue
            sb.append(r.url.trim()).append('\t').append(clean(r.fileName)).append('\t').append(clean(r.folder)).append('\t')
                .append(r.totalBytes).append('\t').append(r.completedAt).append('\t').append(clean(r.category)).append('\n')
        }
        return sb.toString()
    }

    fun parse(text: String): List<FinishedRecord> {
        val out = ArrayList<FinishedRecord>()
        val seen = HashSet<Triple<String, String, String>>()
        for (raw in text.lineSequence()) {
            val line = raw.trim().trimStart('\uFEFF')
            if (line.isEmpty() || line.startsWith("#")) continue
            val p = line.split('\t')
            if (p.size < 3) continue
            val url = p[0].trim()
            val name = p[1].trim()
            val folder = p[2].trim()
            if (!isWeb(url) || name.isEmpty() || name.contains('/') || folder.isEmpty() || !folder.startsWith("/")) continue
            if (folder.split('/').any { it == ".." }) continue
            if (!seen.add(Triple(url, folder, name))) continue
            out.add(FinishedRecord(url, name, folder,
                p.getOrNull(3)?.trim()?.toLongOrNull()?.takeIf { it >= 0 } ?: -1L,
                p.getOrNull(4)?.trim()?.toLongOrNull()?.takeIf { it >= 0 } ?: 0L,
                p.getOrNull(5)?.trim().orEmpty()))
        }
        return out
    }
}
