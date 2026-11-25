package khaled.benzo.core.torrent

import khaled.benzo.core.torrent.StartGrace.Verdict
import org.junit.Assert.assertEquals
import org.junit.Test

class StartGraceTest {
    @Test fun foundIsFound() = assertEquals(Verdict.FOUND, StartGrace(3).onPoll(true))

    @Test fun waitsThenFailsAfterTheLimit() {
        val g = StartGrace(2)
        assertEquals(Verdict.WAIT, g.onPoll(false))
        assertEquals(Verdict.WAIT, g.onPoll(false))
        assertEquals(Verdict.FAIL, g.onPoll(false))
    }

    @Test fun aFoundPollResetsTheCount() {
        val g = StartGrace(1)
        assertEquals(Verdict.WAIT, g.onPoll(false))
        assertEquals(Verdict.FOUND, g.onPoll(true))
        assertEquals(Verdict.WAIT, g.onPoll(false))
    }
}
