package khaled.benzo.core.torrent

import khaled.benzo.core.torrent.SeedingMonitor.Snapshot
import org.junit.Assert.*
import org.junit.Test

class SeedingMonitorTest {
    private val snaps = HashMap<String, Snapshot>()
    private val paused = ArrayList<String>()
    private var ratio = 10
    private var minutes = 0
    private fun monitor() = SeedingMonitor({ ratio }, { minutes }, { snaps[it] }, { paused.add(it) })

    @Test fun pausesOnlyTheTorrentThatReachedTheLimit() {
        snaps["a"] = Snapshot(100, 100, 5); snaps["b"] = Snapshot(50, 100, 5)
        val m = monitor().apply { watch("a"); watch("b") }
        assertEquals(listOf("a"), m.check()); assertEquals(listOf("a"), paused); assertEquals(1, m.watchedCount())
    }

    @Test fun stoppedTorrentIsNotPausedAgain() {
        snaps["a"] = Snapshot(100, 100, 5)
        val m = monitor().apply { watch("a") }
        m.check(); m.check()
        assertEquals(1, paused.size)
    }

    @Test fun unknownTorrentIsDroppedWithoutPausing() {
        val m = monitor().apply { watch("gone") }
        assertTrue(m.check().isEmpty()); assertEquals(0, m.watchedCount()); assertTrue(paused.isEmpty())
    }

    @Test fun noLimitsLeavesEverythingWatched() {
        ratio = 0; minutes = 0
        snaps["a"] = Snapshot(1000, 100, 99999)
        val m = monitor().apply { watch("a") }
        assertTrue(m.check().isEmpty()); assertEquals(1, m.watchedCount()); assertTrue(paused.isEmpty())
    }

    @Test fun limitChangedLaterIsApplied() {
        ratio = 0
        snaps["a"] = Snapshot(100, 100, 0)
        val m = monitor().apply { watch("a") }
        assertTrue(m.check().isEmpty())
        ratio = 10
        assertEquals(listOf("a"), m.check())
    }

    @Test fun timeLimitStops() {
        ratio = 0; minutes = 1
        snaps["a"] = Snapshot(0, 100, 60)
        assertEquals(listOf("a"), monitor().apply { watch("a") }.check())
    }

    @Test fun forgetRemoves() {
        snaps["a"] = Snapshot(100, 100, 5)
        val m = monitor().apply { watch("a"); forget("a") }
        assertTrue(m.check().isEmpty())
    }
}
