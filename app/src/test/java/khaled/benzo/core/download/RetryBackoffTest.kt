package khaled.benzo.core.download

import org.junit.Assert.assertEquals
import org.junit.Test

class RetryBackoffTest {
    @Test fun doublesThenCaps() {
        assertEquals(400L, RetryBackoff.waitMs(400, 0))
        assertEquals(800L, RetryBackoff.waitMs(400, 1))
        assertEquals(RetryBackoff.MAX_MS, RetryBackoff.waitMs(400, 12))
    }
    @Test fun neverOverflowsAtHighAttempts() {
        for (a in listOf(64, 100, 127, 128, 10_000)) assertEquals(RetryBackoff.MAX_MS, RetryBackoff.waitMs(400, a))
    }
    @Test fun zeroBaseMeansNoWait() = assertEquals(0L, RetryBackoff.waitMs(0, 5))
}
