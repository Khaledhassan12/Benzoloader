package khaled.benzo.core.torrent

/** Pure helpers shared by the engine and tests (no native code). */
object TorrentMapping {
    const val PRIORITY_SKIP = 0
    const val PRIORITY_NORMAL = 1
    const val PRIORITY_HIGH = 7

    fun clampPriority(p: Int): Int = when {
        p <= 0 -> PRIORITY_SKIP
        p >= PRIORITY_HIGH -> PRIORITY_HIGH
        else -> p
    }

    /** Priority per file index; unselected files are skipped. */
    fun effectivePriorities(selected: List<Boolean>, priorities: List<Int>): List<Int> =
        selected.indices.map { i ->
            if (!selected[i]) PRIORITY_SKIP else clampPriority(priorities.getOrElse(i) { PRIORITY_NORMAL })
        }

    fun isMagnet(s: String) = s.trim().startsWith("magnet:", ignoreCase = true)

    /** Extracts the 40-hex or 32-base32 btih from a magnet URI, or null. */
    fun magnetInfoHash(uri: String): String? {
        val m = Regex("xt=urn:btih:([A-Fa-f0-9]{40}|[A-Za-z2-7]{32})(?![A-Za-z0-9])", RegexOption.IGNORE_CASE).find(uri) ?: return null
        val v = m.groupValues[1]
        return if (v.length == 40) v.lowercase() else base32ToHex(v)
    }

    private fun base32ToHex(s: String): String? {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        var bits = 0
        var value = 0
        val out = StringBuilder()
        for (ch in s.uppercase()) {
            val idx = alphabet.indexOf(ch)
            if (idx < 0) return null
            value = (value shl 5) or idx
            bits += 5
            if (bits >= 8) {
                bits -= 8
                out.append("%02x".format((value shr bits) and 0xFF))
            }
        }
        return out.toString().takeIf { it.length == 40 }
    }

    fun stateName(engineState: String, paused: Boolean, finished: Boolean): String = when {
        paused -> "PAUSED"
        finished || engineState == "FINISHED" -> "COMPLETED"
        engineState == "SEEDING" -> "SEEDING"
        engineState == "DOWNLOADING_METADATA" -> "FETCHING_METADATA"
        engineState == "CHECKING_FILES" || engineState == "CHECKING_RESUME_DATA" -> "CHECKING"
        engineState == "DOWNLOADING" || engineState == "ALLOCATING" -> "DOWNLOADING"
        else -> "UNKNOWN"
    }
}
