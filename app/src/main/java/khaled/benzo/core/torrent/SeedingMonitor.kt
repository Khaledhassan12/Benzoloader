package khaled.benzo.core.torrent

import java.util.concurrent.ConcurrentHashMap

/**
 * Watches torrents that finished and are still uploading, and pauses them when a seeding limit is reached.
 * Lives in the service, so it keeps working after the task that downloaded the torrent has ended.
 * Only torrents finished during this process are watched: the engine keeps no resume data yet, so seeding does not survive a restart anyway.
 * [stats] and [pause] are the engine calls, passed in so the logic can be tested without libtorrent.
 */
class SeedingMonitor(
    private val ratioTenths: () -> Int,
    private val minutes: () -> Int,
    private val stats: (hash: String) -> Snapshot?,
    private val pause: (hash: String) -> Unit
) {
    data class Snapshot(val uploaded: Long, val size: Long, val seedingSeconds: Long)

    private val watched = ConcurrentHashMap.newKeySet<String>()

    fun watch(hash: String) { watched.add(hash) }
    fun forget(hash: String) { watched.remove(hash) }
    fun watchedCount(): Int = watched.size

    /** One pass over the watched torrents. Returns the hashes it paused. A torrent the engine no longer knows is dropped. */
    fun check(): List<String> {
        if (ratioTenths() <= 0 && minutes() <= 0) return emptyList()
        val stopped = ArrayList<String>()
        for (hash in watched.toList()) {
            val s = stats(hash)
            if (s == null) { watched.remove(hash); continue }
            if (SeedingPolicy.stopReason(ratioTenths(), minutes(), s.uploaded, s.size, s.seedingSeconds) != null) {
                pause(hash)
                watched.remove(hash)
                stopped.add(hash)
            }
        }
        return stopped
    }
}
