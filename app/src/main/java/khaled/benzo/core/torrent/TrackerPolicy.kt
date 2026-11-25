package khaled.benzo.core.torrent

import java.net.URI

/** Rules for tracker addresses a user types in. Pure. A tracker must be http, https or udp with a host. */
object TrackerPolicy {
    const val MAX_LENGTH = 512
    const val MAX_TRACKERS = 100

    sealed class Parsed {
        data class Ok(val urls: List<String>) : Parsed()
        data class Bad(val line: String) : Parsed()
    }

    fun isValid(url: String): Boolean {
        if (url.length > MAX_LENGTH || url.any { it.isWhitespace() || it.code < 32 }) return false
        val u = try { URI(url) } catch (e: Exception) { return false }
        val scheme = u.scheme?.lowercase() ?: return false
        return scheme in setOf("http", "https", "udp") && !u.host.isNullOrBlank()
    }

    /** One tracker per line. Blank lines are skipped, duplicates (ignoring case) are dropped, the first bad line is reported. */
    fun parse(text: String): Parsed {
        val seen = LinkedHashMap<String, String>()
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (!isValid(line)) return Parsed.Bad(line.take(80))
            seen.putIfAbsent(line.lowercase(), line)
        }
        if (seen.size > MAX_TRACKERS) return Parsed.Bad("too many")
        return Parsed.Ok(seen.values.toList())
    }
}
