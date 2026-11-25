package khaled.benzo.core.download

import khaled.benzo.data.model.DownloadItem
import org.junit.Assert.assertEquals
import org.junit.Test

class RequestHeadersTest {
    private fun item(ua: String?, ref: String?, ck: String?) =
        DownloadItem(url = "https://a/b", fileName = "b", savePath = "/x", userAgent = ua, referer = ref, cookies = ck)

    @Test fun blankAndNullAreOmitted() {
        assertEquals(emptyMap<String, String>(), RequestHeaders.from(item(null, " ", "")))
    }

    @Test fun editorFieldsReachTheRequest() {
        assertEquals(mapOf("User-Agent" to "U", "Referer" to "R", "Cookie" to "c=1"), RequestHeaders.from(item("U", "R", "c=1")))
    }
}
