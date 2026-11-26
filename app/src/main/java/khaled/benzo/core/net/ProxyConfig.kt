package khaled.benzo.core.net

import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI

/**
 * One proxy entered as host:port. A "socks5://" prefix makes it a SOCKS proxy; "http://" or no prefix is an HTTP proxy.
 * The reference's proxy types were not visible in the captures, so these two are my own choice (tracked in the log).
 * Sign-in details are NOT accepted: an address containing "@" is rejected, so no credential is ever stored in plain text.
 */
data class ProxyConfig(val socks: Boolean, val host: String, val port: Int) {
    fun label() = (if (socks) "socks5://" else "") + "$host:$port"
    fun toJava(): Proxy = Proxy(if (socks) Proxy.Type.SOCKS else Proxy.Type.HTTP, InetSocketAddress.createUnresolved(host, port))

    sealed interface Parsed { data class Ok(val config: ProxyConfig) : Parsed; object Bad : Parsed }

    companion object {
        private val HOST = Regex("^[A-Za-z0-9]([A-Za-z0-9.-]*[A-Za-z0-9])?$")

        fun parse(text: String): Parsed {
            var t = text.trim()
            if (t.isEmpty() || t.any { it.isWhitespace() } || '@' in t) return Parsed.Bad
            var socks = false
            val lower = t.lowercase()
            when {
                lower.startsWith("socks5://") -> { socks = true; t = t.substring(9) }
                lower.startsWith("http://") -> t = t.substring(7)
                "://" in t -> return Parsed.Bad
            }
            t = t.trimEnd('/')
            val i = t.lastIndexOf(':')
            if (i <= 0) return Parsed.Bad
            val host = t.substring(0, i)
            val port = t.substring(i + 1).toIntOrNull() ?: return Parsed.Bad
            if (port !in 1..65535 || !HOST.matches(host)) return Parsed.Bad
            return Parsed.Ok(ProxyConfig(socks, host.lowercase(), port))
        }
    }
}

/**
 * Reads the chosen proxy on every request, so a change in Settings applies at once without rebuilding the client.
 * With a proxy chosen it returns only that proxy: when it fails the request fails. It never falls back to a direct
 * connection, because that would send the user's traffic around the proxy they asked for.
 */
class AppProxySelector(private val current: () -> ProxyConfig?, private val onChange: () -> Unit = {}) : ProxySelector() {
    private var seen = false
    private var last: String? = null

    /** The first call only records the choice. A later change calls [onChange] (the engine drops idle pooled connections then). */
    override fun select(uri: URI?): List<Proxy> {
        val c = current()
        val label = c?.label()
        val changed = synchronized(this) { val ch = seen && label != last; seen = true; last = label; ch }
        if (changed) onChange()
        return listOf(c?.toJava() ?: Proxy.NO_PROXY)
    }
    override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: java.io.IOException?) {}
}
