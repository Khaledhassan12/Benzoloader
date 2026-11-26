package khaled.benzo.core.data

/**
 * One export file holding several sections (queue, finished, bookmarks, history).
 * Each section starts with a marker line `#@section name`; the section body is the existing
 * per-section text format. Text before the first marker is ignored, unknown names are returned
 * as-is so the caller can skip them. Pure logic. Credentials are never a section.
 */
object ExportBundle {
    const val MAGIC = "# Benzoloader export bundle v1"
    private const val MARK = "#@section "

    fun join(sections: Map<String, String>): String {
        val sb = StringBuilder(MAGIC).append('\n')
        for ((name, body) in sections) {
            require(name.isNotEmpty() && name.none { it.isWhitespace() }) { "bad section name" }
            sb.append(MARK).append(name).append('\n').append(body.trimEnd('\n')).append('\n')
        }
        return sb.toString()
    }

    /** Null when the text is not a bundle (first non-blank line is not the magic line). */
    fun split(text: String): Map<String, String>? {
        val lines = text.removePrefix("\uFEFF").lines().map { it.trimEnd('\r') }.let { if (it.lastOrNull() == "") it.dropLast(1) else it }
        if (lines.firstOrNull { it.isNotBlank() }?.trim() != MAGIC) return null
        val out = LinkedHashMap<String, StringBuilder>()
        var cur: StringBuilder? = null
        for (l in lines.dropWhile { it.isBlank() }.drop(1)) {
            if (l.startsWith(MARK)) {
                val n = l.removePrefix(MARK).trim()
                cur = if (n.isEmpty()) null else out.getOrPut(n) { StringBuilder() }
            } else cur?.append(l)?.append('\n')
        }
        return out.mapValues { it.value.toString() }
    }
}
