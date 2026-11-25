package khaled.benzo.core.torrent

import khaled.benzo.core.torrent.SeedingPolicy.Reason
import org.junit.Assert.*
import org.junit.Test

class SeedingPolicyTest {
    private fun r(ratio: Int, min: Int, up: Long, size: Long, secs: Long) = SeedingPolicy.stopReason(ratio, min, up, size, secs)

    @Test fun noLimitsNeverStops() = assertNull(r(0, 0, 1_000_000L, 10L, 999_999L))

    @Test fun ratioReachedExactlyStops() = assertEquals(Reason.RATIO, r(15, 0, 150L, 100L, 0L))

    @Test fun ratioNotReached() = assertNull(r(15, 0, 149L, 100L, 0L))

    @Test fun zeroSizeNeverTriggersRatio() = assertNull(r(10, 0, 500L, 0L, 0L))

    @Test fun timeReachedExactlyStops() = assertEquals(Reason.TIME, r(0, 2, 0L, 100L, 120L))

    @Test fun timeNotReached() = assertNull(r(0, 2, 0L, 100L, 119L))

    @Test fun firstReachedWins() {
        assertEquals(Reason.RATIO, r(10, 60, 100L, 100L, 10L))
        assertEquals(Reason.TIME, r(50, 1, 10L, 100L, 61L))
    }

    @Test fun negativeUploadIsTreatedAsZero() = assertNull(r(10, 0, -5L, 100L, 0L))

    @Test fun largeValuesDoNotOverflow() = assertEquals(Reason.RATIO, r(100, 0, 5_000_000_000_000L, 500_000_000_000L, 0L))
}
