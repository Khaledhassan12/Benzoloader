package khaled.benzo.core.browser

import org.junit.Assert.*
import org.junit.Test

class HistoryPolicyTest {
    @Test fun offRecordsNothing() = assertFalse(HistoryPolicy.shouldRecord(false, "https://a.com", null))

    @Test fun onlyWebPages() {
        assertTrue(HistoryPolicy.shouldRecord(true, "https://a.com", null))
        assertTrue(HistoryPolicy.shouldRecord(true, "HTTP://a.com", null))
        assertFalse(HistoryPolicy.shouldRecord(true, "about:blank", null))
        assertFalse(HistoryPolicy.shouldRecord(true, "file:///sdcard/x.html", null))
        assertFalse(HistoryPolicy.shouldRecord(true, null, null))
        assertFalse(HistoryPolicy.shouldRecord(true, "  ", null))
    }

    @Test fun sameAddressTwiceInARowIsSkipped() {
        assertFalse(HistoryPolicy.shouldRecord(true, "https://a.com", "https://a.com"))
        assertTrue(HistoryPolicy.shouldRecord(true, "https://a.com", "https://b.com"))
    }

    @Test fun veryLongAddressIsSkipped() =
        assertFalse(HistoryPolicy.shouldRecord(true, "https://a.com/" + "x".repeat(2100), null))

    @Test fun labelFallsBackToAddress() {
        assertEquals("T", HistoryPolicy.label(" T ", "https://a.com"))
        assertEquals("https://a.com", HistoryPolicy.label("  ", "https://a.com"))
        assertEquals("https://a.com", HistoryPolicy.label(null, "https://a.com"))
    }

    @Test fun bookmarkRules() {
        assertTrue(HistoryPolicy.canBookmark("https://a.com", false))
        assertFalse(HistoryPolicy.canBookmark("https://a.com", true))
        assertFalse(HistoryPolicy.canBookmark("about:blank", false))
        assertFalse(HistoryPolicy.canBookmark(null, false))
    }
}
