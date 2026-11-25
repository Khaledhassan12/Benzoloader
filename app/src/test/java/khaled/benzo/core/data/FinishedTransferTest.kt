package khaled.benzo.core.data

import org.junit.Assert.*
import org.junit.Test

class FinishedTransferTest {
    private val r = FinishedRecord("https://a.example/f", "f.bin", "/storage/x", 10L, 99L, "OTHER")
    @Test fun roundTrip() = assertEquals(listOf(r), FinishedTransfer.parse(FinishedTransfer.format(listOf(r))))
    @Test fun rejectsTraversalAndRelative() {
        val t = "https://a.example\tn\t/a/../b\t1\t1\tX\nhttps://a.example\tn\trel\t1\t1\tX\nhttps://a.example\ta/b\t/ok\t1\t1\tX\n"
        assertTrue(FinishedTransfer.parse(t).isEmpty())
    }
    @Test fun dedupes() = assertEquals(1, FinishedTransfer.parse(FinishedTransfer.format(listOf(r, r))).size)
}
