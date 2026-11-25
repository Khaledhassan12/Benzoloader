package khaled.benzo.core.data

import org.junit.Assert.*
import org.junit.Test

class ExportBundleTest {
    @Test fun roundTrip() {
        val m = linkedMapOf("queue" to "https://a.example\tx\n", "bookmarks" to "https://b.example\tt\t1\n")
        assertEquals(m, ExportBundle.split(ExportBundle.join(m)))
    }
    @Test fun plainListIsNotBundle() = assertNull(ExportBundle.split("# Benzoloader download list\nhttps://a.example\n"))
    @Test fun unknownSectionKept() {
        val r = ExportBundle.split(ExportBundle.MAGIC + "\n#@section zzz\nabc\n")!!
        assertEquals("abc\n", r["zzz"])
    }
    @Test(expected = IllegalArgumentException::class) fun badNameRejected() { ExportBundle.join(mapOf("a b" to "x")) }
}
