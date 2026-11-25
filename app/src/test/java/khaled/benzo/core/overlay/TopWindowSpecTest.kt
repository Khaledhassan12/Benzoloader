package khaled.benzo.core.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Written, not run. */
class TopWindowSpecTest {
    private val sample = TopWindowSample(downloadedBytes = 500, totalBytes = 1000, speedBytesPerSec = 100, elapsedSeconds = 65)

    @Test fun showsOnlyWhenEnabledPermittedAndRunning() {
        val on = TopWindowConfig(enabled = true)
        assertTrue(TopWindowSpec.shouldShow(on, true, 1))
        assertFalse(TopWindowSpec.shouldShow(on, true, 0))
        assertFalse(TopWindowSpec.shouldShow(on, false, 1))
        assertFalse(TopWindowSpec.shouldShow(TopWindowConfig(enabled = false), true, 1))
    }
    @Test fun percentAndClamp() {
        assertEquals(50, TopWindowSpec.percent(sample))
        assertEquals(0, TopWindowSpec.percent(sample.copy(totalBytes = -1)))
        assertEquals(100, TopWindowSpec.percent(sample.copy(downloadedBytes = 5000)))
    }
    @Test fun opacityIsClampedToTenToHundred() {
        assertEquals(0.1f, TopWindowSpec.alphaFraction(0), 0.0001f)
        assertEquals(1f, TopWindowSpec.alphaFraction(500), 0.0001f)
    }
    @Test fun textFollowsFixedOrderAndSelection() {
        val c = TopWindowConfig(stats = setOf(TopStat.LEFT, TopStat.PERCENT, TopStat.ELAPSED))
        assertEquals("50.0% \u00B7 01:05 \u00B7 00:05", TopWindowSpec.text(c, sample))
        assertEquals("", TopWindowSpec.text(TopWindowConfig(stats = emptySet()), sample))
    }
    @Test fun unknownTotalAndSpeed() {
        val c = TopWindowConfig(stats = setOf(TopStat.PERCENT, TopStat.LEFT))
        assertEquals("? \u00B7 --:--", TopWindowSpec.text(c, sample.copy(totalBytes = -1, speedBytesPerSec = 0)))
    }
    @Test fun clockHours() = assertEquals("1:01:01", TopWindowSpec.clock(3661))
}
