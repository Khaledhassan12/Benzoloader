package khaled.benzo.core.queue

import khaled.benzo.core.queue.OrderMoves.Move
import khaled.benzo.data.model.DownloadItem
import org.junit.Assert.*
import org.junit.Test

/** WRITTEN, NOT RUN. */
class OrderMovesTest {
    private val s = listOf(1L, 2L, 3L, 4L, 5L)
    @Test fun topAndBottom() {
        assertEquals(listOf(3L, 5L, 1L, 2L, 4L), OrderMoves.move(s, setOf(3L, 5L), Move.TOP))
        assertEquals(listOf(2L, 4L, 5L, 1L, 3L), OrderMoves.move(s, setOf(1L, 3L), Move.BOTTOM))
    }
    @Test fun upAndDownStopAtTheEdges() {
        assertEquals(listOf(2L, 1L, 3L, 4L, 5L), OrderMoves.move(s, setOf(2L), Move.UP))
        assertEquals(s, OrderMoves.move(s, setOf(1L), Move.UP))
        assertEquals(listOf(1L, 2L, 3L, 5L, 4L), OrderMoves.move(s, setOf(4L), Move.DOWN))
        assertEquals(s, OrderMoves.move(s, setOf(5L), Move.DOWN))
    }
    @Test fun contiguousBlockMovesTogether() {
        assertEquals(listOf(1L, 4L, 2L, 3L, 5L), OrderMoves.move(s, setOf(2L, 3L), Move.DOWN))
        assertEquals(listOf(2L, 3L, 1L, 4L, 5L), OrderMoves.move(s, setOf(2L, 3L), Move.UP))
    }
    @Test fun keysKeepTheirRangeAndMatchTheNewOrder() {
        val keys = mapOf(1L to 10L, 2L to 20L, 3L to 30L)
        // screen ascending: 1,2,3 -> after TOP of 3: 3,1,2 gets keys 10,20,30
        val after = OrderMoves.move(listOf(1L, 2L, 3L), setOf(3L), Move.TOP)
        val nk = OrderMoves.newKeys(listOf(1L, 2L, 3L), after, keys, ascending = true)
        assertEquals(mapOf(3L to 10L, 1L to 20L, 2L to 30L), nk)
    }
    @Test fun descendingScreenGivesTheTopTheLargestKey() {
        val keys = mapOf(1L to 10L, 2L to 20L, 3L to 30L)
        val before = listOf(3L, 2L, 1L)
        val after = OrderMoves.move(before, setOf(1L), Move.TOP)   // 1,3,2
        val nk = OrderMoves.newKeys(before, after, keys, ascending = false)
        assertEquals(mapOf(1L to 30L, 3L to 20L, 2L to 10L), nk)
    }
    @Test fun tiedKeysBecomeDistinct() {
        val nk = OrderMoves.newKeys(listOf(1L, 2L), listOf(2L, 1L), mapOf(1L to 5L, 2L to 5L), ascending = true)
        assertEquals(2, nk.size); assertNotEquals(nk[1L], nk[2L]); assertTrue(nk.getValue(2L) < nk.getValue(1L))
    }
    @Test fun orderKeyUsesIdUntilMoved() {
        assertEquals(7L, DownloadItem(id = 7, url = "u", fileName = "f", savePath = "/p").orderKey)
        assertEquals(99L, DownloadItem(id = 7, url = "u", fileName = "f", savePath = "/p", listOrder = 99).orderKey)
    }
}
