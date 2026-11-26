package khaled.benzo.core.download

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/** WRITTEN, NOT RUN. Fake-server checks for the size/name probe the Editor uses (DownloadEngine.checkServer). */
class ProbeCheckServerTest {
    private lateinit var server: MockWebServer
    @Before fun up() { server = MockWebServer(); server.start() }
    @After fun down() { server.shutdown() }

    private fun shortClient() = DownloadEngine.createDefaultClient().newBuilder()
        .connectTimeout(1, TimeUnit.SECONDS).readTimeout(1, TimeUnit.SECONDS).callTimeout(2, TimeUnit.SECONDS).build()

    @Test fun rangeIgnoredStillReturnsLengthWithoutReadingBody() = runBlocking {
        val body = Buffer().write(ByteArray(4 * 1024 * 1024))
        server.enqueue(MockResponse().setResponseCode(200).setBody(body).throttleBody(1024, 100, TimeUnit.MILLISECONDS))
        val t0 = System.currentTimeMillis()
        val info = DownloadEngine(shortClient()).checkServer(server.url("/f.bin").toString(), emptyMap())
        assertEquals(4L * 1024 * 1024, info.contentLength)
        assertFalse(info.supportsRange)
        assertTrue("probe took too long: ${System.currentTimeMillis() - t0} ms", System.currentTimeMillis() - t0 < 2500)
    }

    @Test fun suggestedNameFromDisposition() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(206).setHeader("Content-Range", "bytes 0-0/1000")
            .setHeader("Content-Disposition", "attachment; filename=\"report.pdf\"").setBody("x"))
        val info = DownloadEngine(shortClient()).checkServer(server.url("/dl").toString(), emptyMap())
        assertEquals(1000L, info.contentLength)
        assertTrue(info.supportsRange)
        assertEquals("report.pdf", info.suggestedFileName)
    }

    @Test fun sendsGivenUserAgentAndNoCookies() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("x"))
        DownloadEngine(shortClient()).checkServer(server.url("/a").toString(), mapOf("User-Agent" to "TestUA/1"))
        val req = server.takeRequest()
        assertEquals("TestUA/1", req.getHeader("User-Agent"))
        assertNull(req.getHeader("Cookie"))
        assertEquals("bytes=0-0", req.getHeader("Range"))
    }

    @Test fun slowServerFailsWithinTimeout() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("x").setHeadersDelay(5, TimeUnit.SECONDS))
        val t0 = System.currentTimeMillis()
        val failed = try { runBlocking { DownloadEngine(shortClient()).checkServer(server.url("/s").toString(), emptyMap()) }; false } catch (e: Exception) { true }
        assertTrue(failed)
        assertTrue(System.currentTimeMillis() - t0 < 4000)
    }
}
