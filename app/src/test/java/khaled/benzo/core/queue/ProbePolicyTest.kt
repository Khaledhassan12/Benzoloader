package khaled.benzo.core.queue

import org.junit.Assert.*
import org.junit.Test

/** WRITTEN, NOT RUN. */
class ProbePolicyTest {
    @Test fun noNetworkBlocks() = assertEquals(ProbePolicy.Decision.NO_NETWORK, ProbePolicy.decide(false, true, false, false))
    @Test fun wifiOnlyOnMobileBlocks() {
        assertEquals(ProbePolicy.Decision.WAITING_FOR_WIFI, ProbePolicy.decide(true, false, true, false))
        assertEquals(ProbePolicy.Decision.WAITING_FOR_WIFI, ProbePolicy.decide(true, false, false, true))
    }
    @Test fun allowedOtherwise() {
        assertEquals(ProbePolicy.Decision.ALLOW, ProbePolicy.decide(true, true, true, true))
        assertEquals(ProbePolicy.Decision.ALLOW, ProbePolicy.decide(true, false, false, false))
    }
    @Test fun gateDedupesSameLinkUnlessForced() {
        val g = ProbeGate()
        assertTrue(g.begin("https://a/x")); assertFalse(g.begin("https://a/x")); assertTrue(g.begin("https://a/x", force = true))
        assertTrue(g.begin("https://a/y"))
    }
    @Test fun staleResultRejected() {
        val g = ProbeGate()
        assertTrue(g.isCurrent("https://a/x", " https://a/x ")); assertFalse(g.isCurrent("https://a/x", "https://a/y"))
    }
}
