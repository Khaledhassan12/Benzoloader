package khaled.benzo.core.notify

import org.junit.Assert.assertEquals
import org.junit.Test

class VibrationPolicyTest {
    @Test fun offMeansZero() = assertEquals(0L, VibrationPolicy.durationMs(false, 300))
    @Test fun valueIsUsed() = assertEquals(150L, VibrationPolicy.durationMs(true, 150))
    @Test fun valueIsClamped() {
        assertEquals(1000L, VibrationPolicy.durationMs(true, 99999))
        assertEquals(50L, VibrationPolicy.durationMs(true, -5))
    }
}
