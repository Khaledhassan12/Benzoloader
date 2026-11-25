package khaled.benzo.core.download

import org.junit.Assert.*
import org.junit.Test

class RetryAfterTest {
    @Test fun seconds() = assertEquals(5_000L, RetryAfter.parseMs("5", 0))
    @Test fun secondsAreCapped() = assertEquals(60_000L, RetryAfter.parseMs("86400", 0))
    @Test fun httpDateInTheFuture() {
        // Wed, 21 Oct 2015 07:28:00 GMT = 1445412480000 ms
        assertEquals(10_000L, RetryAfter.parseMs("Wed, 21 Oct 2015 07:28:00 GMT", 1445412470000L))
    }
    @Test fun httpDateInThePastMeansNoWait() = assertEquals(0L, RetryAfter.parseMs("Wed, 21 Oct 2015 07:28:00 GMT", 1445412500000L))
    @Test fun garbageNegativeAndMissingGiveNull() {
        assertNull(RetryAfter.parseMs("soon", 0)); assertNull(RetryAfter.parseMs("-3", 0)); assertNull(RetryAfter.parseMs(null, 0)); assertNull(RetryAfter.parseMs("  ", 0))
    }
    @Test fun transientStatuses() {
        for (c in listOf(408, 429, 500, 503)) assertTrue(c.toString(), TransientStatus.isTransient(c))
        for (c in listOf(400, 401, 403, 404, 410, 416)) assertFalse(c.toString(), TransientStatus.isTransient(c))
    }
}
