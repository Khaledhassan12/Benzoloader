package khaled.benzo.core.queue

import org.junit.Assert.*
import org.junit.Test

/** WRITTEN, NOT RUN. */
class SelectionStateTest {
    @Test fun toggleAddsAndRemoves() {
        val s = SelectionState().toggle(1).toggle(2)
        assertEquals(setOf(1L, 2L), s.selected); assertTrue(s.active)
        assertEquals(setOf(2L), s.toggle(1).selected)
        assertFalse(s.toggle(1).toggle(2).active)
    }
    @Test fun selectAllAndInvert() {
        val all = listOf(1L, 2L, 3L)
        assertEquals(3, SelectionState().selectAll(all).count)
        assertEquals(setOf(2L, 3L), SelectionState(setOf(1L)).invert(all).selected)
    }
    @Test fun pruneDropsMissingIds() {
        assertEquals(setOf(2L), SelectionState(setOf(1L, 2L)).prune(listOf(2L, 3L)).selected)
        assertFalse(SelectionState(setOf(1L)).prune(emptyList()).active)
    }
}
