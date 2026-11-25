package khaled.benzo.core.torrent

/** Pure rules for the torrent network settings, so they can be unit-tested without the native library. */
object TorrentNetworkPolicy {
    const val MAX_CONNECTIONS = 1000
    const val MIN_CONNECTIONS = 10

    /** 0 (or less) means "leave the library's own value". Otherwise a bounded value. */
    fun connectionsToApply(setting: Int, libraryDefault: Int): Int =
        if (setting <= 0) libraryDefault else setting.coerceIn(MIN_CONNECTIONS, MAX_CONNECTIONS)
}
