package khaled.benzo.core.data

import org.junit.Assert.*
import org.junit.Test

class PageListTransferTest {
    private val h = PageListTransfer.BOOKMARKS_HEADER
    @Test fun roundTrip() {
        val e = listOf(PageEntry("https://a.example/x", "A\tB", 5L))
        val back = PageListTransfer.parse(PageListTransfer.format(h, e))
        assertEquals(listOf(PageEntry("https://a.example/x", "A B", 5L)), back)
    }
    @Test fun skipsNonWebAndDuplicates() {
        val t = "# c\nftp://x/y\thi\t1\nhttps://a.example\tt\t1\nhttps://a.example\tdup\t2\nhttp://b.example\n"
        val r = PageListTransfer.parse(t)
        assertEquals(2, r.size); assertEquals(0L, r[1].time)
    }
    @Test fun formatDropsNonWeb() = assertFalse(PageListTransfer.format(h, listOf(PageEntry("file:///x", "t", 1))).contains("file:"))
}
