package khaled.benzo.core.torrent

import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Rules for torrent sources that arrive from other apps (open/share intents). Pure and testable.
 * Magnet links and content:// URIs stay allowed. file:// and plain paths must point into shared storage,
 * never into this app's own data. Every read is size-bounded and must look like a bencoded dictionary.
 */
object TorrentSourcePolicy {
    /** Real .torrent files are small. Larger input is refused instead of being read into memory. */
    const val MAX_BYTES = 8 * 1024 * 1024

    enum class Kind { MAGNET, CONTENT, FILE, REJECTED }

    fun classify(s: String): Kind {
        val t = s.trim()
        val lower = t.lowercase()
        return when {
            lower.startsWith("magnet:") -> Kind.MAGNET
            lower.startsWith("content://") -> Kind.CONTENT
            lower.startsWith("file://") -> Kind.FILE
            t.startsWith("/") -> Kind.FILE
            else -> Kind.REJECTED
        }
    }

    /** [canonicalPath] must already be resolved (no "..", symlinks followed). Roots are canonical directory paths. */
    fun isFileAllowed(canonicalPath: String, allowedRoots: List<String>, deniedRoots: List<String>): Boolean {
        if (deniedRoots.any { isInside(canonicalPath, it) }) return false
        return allowedRoots.any { isInside(canonicalPath, it) }
    }

    fun isInside(path: String, root: String): Boolean {
        val r = root.trimEnd('/')
        return path == r || path.startsWith("$r/")
    }

    /** A .torrent is a bencoded dictionary: starts with 'd' and ends with 'e'. */
    fun looksLikeTorrent(bytes: ByteArray): Boolean =
        bytes.size >= 2 && bytes.first() == 'd'.code.toByte() && bytes.last() == 'e'.code.toByte()

    /** Reads at most [MAX_BYTES]. Returns null when the stream is larger. */
    fun readLimited(input: InputStream, max: Int = MAX_BYTES): ByteArray? {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        var total = 0
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            total += n
            if (total > max) return null
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }
}
