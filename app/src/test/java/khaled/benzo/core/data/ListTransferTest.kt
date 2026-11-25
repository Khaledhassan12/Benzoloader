package khaled.benzo.core.data

import khaled.benzo.data.model.DownloadItem
import org.junit.Assert.*
import org.junit.Test

class ListTransferTest {
    private fun item(url: String, name: String) =
        DownloadItem(url = url, fileName = name, savePath = "/x", cookies = "SECRET=1", referer = "http://r", headersJson = "{\"A\":\"B\"}", userAgent = "UA")

    @Test fun exportNeverContainsSessionData() {
        val t = ListTransfer.format(listOf(item("https://a.com/f.zip", "f.zip")))
        assertFalse(t.contains("SECRET")); assertFalse(t.contains("http://r")); assertFalse(t.contains("UA\n"))
    }

    @Test fun roundTrip() {
        val items = listOf(item("https://a.com/f.zip", "f.zip"), item("http://b.org/v.mp4", "v.mp4"))
        val back = ListTransfer.parse(ListTransfer.format(items))
        assertEquals(listOf(ImportEntry("https://a.com/f.zip", "f.zip"), ImportEntry("http://b.org/v.mp4", "v.mp4")), back)
    }

    @Test fun skipsTorrentsCommentsBlanksAndUnsupportedSchemes() {
        val t = "# c\n\nftp://x/y\nhttps://ok.com/a\njavascript:alert(1)\nhttps://\nhttps://has space/x\n"
        assertEquals(listOf(ImportEntry("https://ok.com/a", null)), ListTransfer.parse(t))
    }

    @Test fun dedupesAndHandlesCrlfAndBom() {
        val t = "\uFEFFhttps://a.com/1\r\nhttps://a.com/1\t other\r\nHTTPS://a.com/2\r\n"
        assertEquals(listOf("https://a.com/1", "HTTPS://a.com/2"), ListTransfer.parse(t).map { it.url })
    }

    @Test fun tabInNameIsFlattened() {
        val t = ListTransfer.format(listOf(item("https://a.com/f", "a\tb")))
        assertEquals("a b", ListTransfer.parse(t)[0].fileName)
    }
}

class ListTransferNameTest {
    @Test fun nameFromUrl() {
        assertEquals("a b.zip", ListTransfer.nameFor(ImportEntry("https://h.com/d/a%20b.zip?x=1", null)))
        assertEquals("download", ListTransfer.nameFor(ImportEntry("https://h.com", null)))
        assertEquals("given.bin", ListTransfer.nameFor(ImportEntry("https://h.com/z", "given.bin")))
    }
}
