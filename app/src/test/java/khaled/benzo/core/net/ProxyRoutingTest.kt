package khaled.benzo.core.net

import khaled.benzo.core.download.DownloadEngine
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.net.ServerSocket
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * WRITTEN, NOT RUN. These are checks of hypotheses, not facts: that the shared client really sends link traffic through the
 * chosen proxy, never goes direct when the proxy fails, follows redirects through it, drops pooled connections when the
 * choice changes, and leaves DNS to a SOCKS proxy. Treat the claims as unverified until they have run.
 */
class ProxyRoutingTest {
    private lateinit var proxyServer: MockWebServer
    private lateinit var origin: MockWebServer
    @Before fun up() { proxyServer = MockWebServer().apply { start() }; origin = MockWebServer().apply { start() } }
    @After fun down() { proxyServer.shutdown(); origin.shutdown() }

    private fun client(p: () -> ProxyConfig?) = DownloadEngine.createDefaultClient(p).newBuilder()
        .connectTimeout(2, TimeUnit.SECONDS).readTimeout(2, TimeUnit.SECONDS).callTimeout(4, TimeUnit.SECONDS).build()
    private fun httpProxy(s: MockWebServer) = ProxyConfig(false, s.hostName, s.port)

    @Test fun httpRequestGoesToTheProxyNotTheOrigin() {
        proxyServer.enqueue(MockResponse().setBody("via proxy"))
        val r = client { httpProxy(proxyServer) }.newCall(Request.Builder().url("http://origin.test/file").build()).execute()
        assertEquals("via proxy", r.body!!.string())
        assertTrue(proxyServer.takeRequest().requestLine.startsWith("GET http://origin.test/file"))
        assertEquals(0, origin.requestCount)
    }

    @Test fun deadProxyFailsTheRequestAndNeverGoesDirect() {
        origin.enqueue(MockResponse().setBody("direct"))
        val dead = ServerSocket(0).use { it.localPort }
        val c = client { ProxyConfig(false, "127.0.0.1", dead) }
        try { c.newCall(Request.Builder().url(origin.url("/x")).build()).execute().close(); fail("request must fail") } catch (e: java.io.IOException) {}
        assertEquals("nothing may reach the origin directly", 0, origin.requestCount)
    }

    @Test fun redirectIsFollowedThroughTheProxyToo() {
        proxyServer.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "http://other.test/b"))
        proxyServer.enqueue(MockResponse().setBody("ok"))
        val r = client { httpProxy(proxyServer) }.newCall(Request.Builder().url("http://origin.test/a").build()).execute()
        assertEquals("ok", r.body!!.string())
        assertEquals(2, proxyServer.requestCount); assertEquals(0, origin.requestCount)
    }

    @Test fun changingTheChoiceDropsIdlePooledConnections() {
        origin.enqueue(MockResponse().setBody("1")); origin.enqueue(MockResponse().setBody("2"))
        val choice = AtomicReference<ProxyConfig?>(null)
        val c = client { choice.get() }
        c.newCall(Request.Builder().url(origin.url("/a")).build()).execute().use { it.body!!.string() }
        assertTrue(c.connectionPool.connectionCount() >= 1)
        choice.set(httpProxy(proxyServer))
        proxyServer.enqueue(MockResponse().setBody("p"))
        c.newCall(Request.Builder().url("http://origin.test/b").build()).execute().use { it.body!!.string() }
        assertEquals(1, proxyServer.requestCount); assertEquals(1, origin.requestCount)
    }

    @Test fun selectorReportsAChangeOnlyWhenTheChoiceChanges() {
        val n = AtomicInteger(); val choice = AtomicReference<ProxyConfig?>(null)
        val sel = AppProxySelector({ choice.get() }, { n.incrementAndGet() })
        sel.select(null); sel.select(null); assertEquals(0, n.get())
        choice.set(ProxyConfig(false, "h.example", 1)); sel.select(null); sel.select(null); assertEquals(1, n.get())
    }

    /** A tiny SOCKS5 listener that records how the target was named, then refuses. A name (type 3) means DNS is left to the proxy. */
    @Test fun socksProxyReceivesTheHostNameNotALocalLookup() {
        val atyp = AtomicInteger(-1); val host = AtomicReference<String?>(null)
        val ss = ServerSocket(0)
        val t = Thread {
            ss.accept().use { s ->
                val i = s.getInputStream(); val o = s.getOutputStream()
                i.read(); val n = i.read(); repeat(n) { i.read() }
                o.write(byteArrayOf(5, 0)); o.flush()
                i.read(); i.read(); i.read(); atyp.set(i.read())
                if (atyp.get() == 3) { val len = i.read(); host.set(String(ByteArray(len).also { b -> i.readNBytes(b, 0, len) })) }
                o.write(byteArrayOf(5, 1, 0, 1, 0, 0, 0, 0, 0, 0)); o.flush()
            }
        }.apply { start() }
        try {
            client { ProxyConfig(true, "127.0.0.1", ss.localPort) }.newCall(Request.Builder().url("http://dns-leak-check.invalid/").build()).execute().close()
            fail("the proxy refuses, so the request must fail")
        } catch (e: java.io.IOException) {}
        t.join(3000); ss.close()
        assertEquals(3, atyp.get()); assertEquals("dns-leak-check.invalid", host.get())
    }

    @Test fun coverageListNamesTorrentsAndBrowserAsDirect() {
        val direct = ProxyCoverage.direct()
        assertTrue(ProxyCoverage.Route.TORRENTS in direct && ProxyCoverage.Route.BROWSER in direct)
        assertEquals(ProxyCoverage.State.PROXIED, ProxyCoverage.routes[ProxyCoverage.Route.LINK_DOWNLOADS])
    }
}
