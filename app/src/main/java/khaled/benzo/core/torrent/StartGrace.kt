package khaled.benzo.core.torrent

/**
 * jlibtorrent adds a torrent asynchronously, so for a moment after adding it the session may not know the torrent yet.
 * That moment must not count as a failure. This allows [maxMisses] polls in a row without a handle before giving up,
 * and starts counting again after every poll that finds it.
 */
class StartGrace(private val maxMisses: Int) {
    enum class Verdict { FOUND, WAIT, FAIL }

    private var misses = 0

    fun onPoll(found: Boolean): Verdict {
        if (found) { misses = 0; return Verdict.FOUND }
        misses++
        return if (misses > maxMisses) Verdict.FAIL else Verdict.WAIT
    }
}
