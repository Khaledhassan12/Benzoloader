package khaled.benzo.core.torrent

/** Pure rules for the torrent network settings, so they can be unit-tested without the native library. */
object TorrentNetworkPolicy {
    const val MAX_CONNECTIONS = 1000
    const val MIN_CONNECTIONS = 10

    const val MIN_PORT = 1024
    const val MAX_PORT = 65535

    /** 0 means "leave the library's own listen setting". Otherwise a port inside the unprivileged range, or 0 when it is out of range. */
    fun portToApply(fixed: Boolean, port: Int): Int = if (fixed && port in MIN_PORT..MAX_PORT) port else 0

    /** libtorrent listen_interfaces text for one fixed port on IPv4 and IPv6. */
    fun listenInterfaces(port: Int): String = "0.0.0.0:$port,[::]:$port"

    /** 0 (or less) means "leave the library's own value". Otherwise a bounded value. */
    fun connectionsToApply(setting: Int, libraryDefault: Int): Int =
        if (setting <= 0) libraryDefault else setting.coerceIn(MIN_CONNECTIONS, MAX_CONNECTIONS)
}
