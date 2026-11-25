package khaled.benzo.core.torrent

import org.junit.Assert.*
import org.junit.Test

class TrackerPolicyTest {
    @Test fun acceptsCommonSchemes() {
        assertTrue(TrackerPolicy.isValid("udp://tracker.example.org:6969/announce"))
        assertTrue(TrackerPolicy.isValid("https://t.example.org/announce"))
        assertTrue(TrackerPolicy.isValid("http://t.example.org:80/announce"))
    }
    @Test fun refusesOthers() {
        assertFalse(TrackerPolicy.isValid("ftp://x.example.org/a"))
        assertFalse(TrackerPolicy.isValid("file:///etc/passwd"))
        assertFalse(TrackerPolicy.isValid("http://"))
        assertFalse(TrackerPolicy.isValid("udp://a b.example.org"))
        assertFalse(TrackerPolicy.isValid("javascript:alert(1)"))
        assertFalse(TrackerPolicy.isValid("http://x.example.org/" + "a".repeat(600)))
    }
    @Test fun parseDedupesAndSkipsBlank() {
        val r = TrackerPolicy.parse("udp://a.example.org:1/x\n\n UDP://A.example.org:1/x \nhttp://b.example.org/y")
        assertEquals(listOf("udp://a.example.org:1/x", "http://b.example.org/y"), (r as TrackerPolicy.Parsed.Ok).urls)
    }
    @Test fun parseReportsFirstBadLine() {
        val r = TrackerPolicy.parse("udp://a.example.org:1/x\nnot a url")
        assertEquals("not a url", (r as TrackerPolicy.Parsed.Bad).line)
    }
}
