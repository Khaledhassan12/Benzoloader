package khaled.benzo.core.download

/** Server-side identity of a file, used to detect that it changed between pause and resume. */
data class Validator(val etag: String?, val lastModified: String?, val length: Long) {

    /** True when we have something to compare against. Weak ETags are ignored (not byte-exact). */
    val usable: Boolean get() = !strongEtag().isNullOrBlank() || !lastModified.isNullOrBlank()

    fun strongEtag(): String? = etag?.trim()?.takeIf { it.isNotEmpty() && !it.startsWith("W/", ignoreCase = true) }

    fun serialize(): String = listOf(strongEtag().orEmpty(), lastModified.orEmpty(), length.toString()).joinToString("\n")

    companion object {
        fun parse(text: String?): Validator? {
            if (text == null) return null
            val l = text.split("\n")
            if (l.size < 3) return null
            return Validator(l[0].ifEmpty { null }, l[1].ifEmpty { null }, l[2].trim().toLongOrNull() ?: return null)
        }
    }
}

object ResumePolicy {
    /**
     * Adds If-Range (RFC 9110, section 13.1.5) built from the stored validator: the strong ETag when there is one, otherwise the
     * stored Last-Modified date. Nothing is added when there is nothing usable, or when the request already has an If-Range.
     */
    fun withIfRange(headers: Map<String, String>, stored: Validator?): Map<String, String> {
        if (stored == null || !stored.usable || headers.keys.any { it.equals("If-Range", ignoreCase = true) }) return headers
        val value = stored.strongEtag() ?: stored.lastModified?.trim()?.takeIf { it.isNotEmpty() } ?: return headers
        return headers + ("If-Range" to value)
    }

    /**
     * Whether it is safe to continue with the saved chunks. Unknown or unverifiable cases return true so a flaky
     * check never throws away progress; only a positive mismatch restarts the download.
     */
    fun canResume(stored: Validator?, current: Validator?): Boolean {
        if (stored == null || current == null || !stored.usable) return true
        if (stored.length > 0 && current.length > 0 && stored.length != current.length) return false
        val a = stored.strongEtag()
        val b = current.strongEtag()
        if (a != null && b != null) return a == b
        if (!stored.lastModified.isNullOrBlank() && !current.lastModified.isNullOrBlank()) {
            return stored.lastModified == current.lastModified
        }
        return true
    }
}

object ChunkPlanner {
    const val MIN_CHUNK_BYTES = 512L * 1024L

    /** Number of parallel parts: never more than requested, never chunks smaller than [minChunk], 1 without range support. */
    fun partsFor(totalSize: Long, requestedThreads: Int, supportsRange: Boolean, minChunk: Long = MIN_CHUNK_BYTES): Int {
        if (!supportsRange || totalSize <= 0L) return 1
        val bySize = (totalSize / minChunk).coerceAtLeast(1L)
        return minOf(requestedThreads.coerceIn(1, 16).toLong(), bySize).toInt()
    }

    /** Inclusive [start,end] ranges that exactly cover [0,totalSize). */
    fun ranges(totalSize: Long, parts: Int): List<LongRange> {
        if (parts <= 1 || totalSize <= 0L) return listOf(0L..(if (totalSize > 0) totalSize - 1 else -1L))
        val size = totalSize / parts
        return (0 until parts).map { i ->
            val start = i * size
            val end = if (i == parts - 1) totalSize - 1 else start + size - 1
            start..end
        }
    }
}
