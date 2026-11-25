package khaled.benzo.core.queue

import khaled.benzo.core.download.PermanentDownloadException
import khaled.benzo.core.download.RangeIgnoredException
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class AutoResumePolicyTest {
    @Test fun disabledNeverResumes() = assertFalse(AutoResumePolicy.shouldResume(false, 0, IOException("reset")))
    @Test fun transientFailureResumes() = assertTrue(AutoResumePolicy.shouldResume(true, 0, IOException("reset")))
    @Test fun unknownFailureResumes() = assertTrue(AutoResumePolicy.shouldResume(true, 1, null))
    @Test fun stopsAfterThreeAutomaticAttempts() {
        assertTrue(AutoResumePolicy.shouldResume(true, 2, IOException("x")))
        assertFalse(AutoResumePolicy.shouldResume(true, 3, IOException("x")))
    }
    @Test fun permanentFailuresAreNotRetried() {
        assertFalse(AutoResumePolicy.shouldResume(true, 0, PermanentDownloadException("HTTP 404")))
        assertFalse(AutoResumePolicy.shouldResume(true, 0, RangeIgnoredException("200")))
        assertFalse(AutoResumePolicy.shouldResume(true, 0, IOException("Not enough free storage: need 5 bytes")))
    }
    @Test fun delayDoublesFrom30Seconds() {
        assertEquals(30_000L, AutoResumePolicy.delayMs(0))
        assertEquals(60_000L, AutoResumePolicy.delayMs(1))
        assertEquals(120_000L, AutoResumePolicy.delayMs(2))
    }

    @Test fun serverWaitIsHonouredNeverShortened() {
        val f = khaled.benzo.core.download.TransientHttpException(429, 300_000L)
        assertTrue(AutoResumePolicy.shouldResume(true, 0, f))
        assertEquals(300_000L, AutoResumePolicy.delayMs(0, f))      // server asked 5 min, backoff is 30 s
        assertEquals(120_000L, AutoResumePolicy.delayMs(2, khaled.benzo.core.download.TransientHttpException(503, 10_000L)))
    }
    @Test fun serverWaitLongerThanFifteenMinutesStopsAutomaticRetry() {
        assertTrue(AutoResumePolicy.shouldResume(true, 0, khaled.benzo.core.download.TransientHttpException(429, 15 * 60_000L)))
        assertFalse(AutoResumePolicy.shouldResume(true, 0, khaled.benzo.core.download.TransientHttpException(429, 15 * 60_000L + 1)))
    }
}
