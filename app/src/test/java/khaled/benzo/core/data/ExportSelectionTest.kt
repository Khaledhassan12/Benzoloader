package khaled.benzo.core.data

import org.junit.Assert.*
import org.junit.Test

class ExportSelectionTest {
    @Test fun emptyDisablesOk() = assertFalse(ExportSelection().okEnabled)
    @Test fun toggleTwiceClears() {
        val s = ExportSelection().toggle(ExportItem.QUEUE).toggle(ExportItem.QUEUE)
        assertFalse(s.okEnabled)
    }
    @Test fun countSumsTicked() {
        val s = ExportSelection().toggle(ExportItem.FINISHED).toggle(ExportItem.QUEUE)
        assertEquals(12, s.okCount(mapOf(ExportItem.FINISHED to 10, ExportItem.QUEUE to 2, ExportItem.HISTORY to 99)))
    }
    @Test fun authorizationNeverWritten() {
        val s = ExportSelection().toggle(ExportItem.AUTHORIZATION).toggle(ExportItem.HISTORY)
        assertEquals(setOf(ExportItem.HISTORY), s.writable())
    }
}
