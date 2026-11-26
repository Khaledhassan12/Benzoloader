package khaled.benzo.core.net

import org.junit.Assert.*
import org.junit.Test
import java.net.Proxy

/** WRITTEN, NOT RUN. */
class ProxyConfigTest {
    private fun ok(s: String) = (ProxyConfig.parse(s) as ProxyConfig.Parsed.Ok).config
    @Test fun hostPortIsHttp() { val c = ok(" Example.org:8080 "); assertFalse(c.socks); assertEquals("example.org", c.host); assertEquals(8080, c.port) }
    @Test fun socksPrefix() { assertTrue(ok("socks5://10.0.0.1:1080").socks); assertEquals("socks5://10.0.0.1:1080", ok("socks5://10.0.0.1:1080").label()) }
    @Test fun httpPrefixAccepted() = assertFalse(ok("http://p.example:3128/").socks)
    @Test fun badInputsRejected() {
        for (s in listOf("", "host", "host:0", "host:70000", "host:abc", "ftp://h:1", "user:pw@h:1", "a b:1", ":80", "-h:80", "h_x:80"))
            assertSame(s, ProxyConfig.Parsed.Bad, ProxyConfig.parse(s))
    }
    @Test fun selectorNeverFallsBackToDirectWhenProxyChosen() {
        val sel = AppProxySelector { ok("h.example:1") }
        assertEquals(1, sel.select(null).size); assertEquals(Proxy.Type.HTTP, sel.select(null)[0].type())
        assertEquals(Proxy.NO_PROXY, AppProxySelector { null }.select(null)[0])
    }
}
