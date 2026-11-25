package khaled.benzo.core.download

import org.junit.Assert.*
import org.junit.Test

class RetryAfterTest {
    // Wed, 21 Oct 2015 07:28:00 GMT = 1445412480000 ms
    private val date = "Wed, 21 Oct 2015 07:28:00 GMT"
    private val at = 1445412480000L

    @Test fun seconds() = assertEquals(5_000L, RetryAfter.parseMs("5", null, 0))
    @Test fun longSecondsAreNotShortened() = assertEquals(86_400_000L, RetryAfter.parseMs("86400", null, 0))
    @Test fun absurdSecondsAreOnlyLimitedToAYear() = assertEquals(366L * 24 * 3600 * 1000, RetryAfter.parseMs("9999999999999", null, 0))
    @Test fun dateUsesTheServersOwnClockWhenGiven() {
        // phone clock is wrong by an hour, server Date header is 10 s before the retry time
        assertEquals(10_000L, RetryAfter.parseMs(date, at - 10_000, at + 3_600_000))
    }
    @Test fun dateFallsBackToThePhoneClock() = assertEquals(10_000L, RetryAfter.parseMs(date, null, at - 10_000))
    @Test fun dateInThePastMeansNoWait() = assertEquals(0L, RetryAfter.parseMs(date, null, at + 5_000))
    @Test fun zeroSecondsIsZero() = assertEquals(0L, RetryAfter.parseMs("0", null, 0))
    @Test fun garbageNegativeAndMissingGiveNull() {
        assertNull(RetryAfter.parseMs("soon", null, 0)); assertNull(RetryAfter.parseMs("-3", null, 0))
        assertNull(RetryAfter.parseMs(null, null, 0)); assertNull(RetryAfter.parseMs("  ", null, 0))
        assertNull(RetryAfter.parseMs("1.5", null, 0))
    }
    @Test fun transientStatuses() {
        for (c in listOf(408, 429, 500, 503)) assertTrue(c.toString(), TransientStatus.isTransient(c))
        for (c in listOf(400, 401, 403, 404, 410, 416)) assertFalse(c.toString(), TransientStatus.isTransient(c))
    }
}
