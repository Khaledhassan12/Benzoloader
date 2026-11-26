package khaled.benzo.core.queue

import org.junit.Assert.*
import org.junit.Test

// Written, not run.
class AfterFinishPolicyTest {
    private val exit = AfterFinish.EXIT_SERVICE
    @Test fun exitsOnlyWhenEverythingIsDone() {
        assertTrue(AfterFinishPolicy.shouldExit(exit, true, 0, 0, 0))
        assertFalse(AfterFinishPolicy.shouldExit(exit, true, 1, 0, 0))
        assertFalse(AfterFinishPolicy.shouldExit(exit, true, 0, 2, 0))
        assertFalse(AfterFinishPolicy.shouldExit(exit, true, 0, 0, 1))
    }
    @Test fun neverExitsAfterAFailureOrWhenOff() {
        assertFalse(AfterFinishPolicy.shouldExit(exit, false, 0, 0, 0))
        assertFalse(AfterFinishPolicy.shouldExit(AfterFinish.NOTHING, true, 0, 0, 0))
    }
    @Test fun unknownCodeMeansNothing() {
        assertEquals(AfterFinish.NOTHING, AfterFinish.fromCode(7))
        assertEquals(AfterFinish.EXIT_SERVICE, AfterFinish.fromCode(1))
    }

    /** WRITTEN, NOT RUN. */
    @Test fun allFinishedNeedsNothingElseRunningWaitingOrSeeding() {
        assertTrue(AfterFinishPolicy.allFinished(true, 0, 0, 0))
        assertFalse(AfterFinishPolicy.allFinished(false, 0, 0, 0))
        assertFalse(AfterFinishPolicy.allFinished(true, 1, 0, 0))
        assertFalse(AfterFinishPolicy.allFinished(true, 0, 2, 0))
        assertFalse(AfterFinishPolicy.allFinished(true, 0, 0, 1))
    }
}
