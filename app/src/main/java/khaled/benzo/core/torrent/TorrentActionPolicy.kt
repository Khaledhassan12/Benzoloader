package khaled.benzo.core.torrent

import khaled.benzo.data.model.DownloadItem

/** Which selected items the torrent-only actions apply to: torrents with a known hash. Whether the engine has them loaded is checked at the call. */
object TorrentActionPolicy {
    fun targets(items: List<DownloadItem>): List<DownloadItem> = items.filter { it.isTorrent && !it.torrentInfoHash.isNullOrBlank() }
    fun showActions(items: List<DownloadItem>) = targets(items).isNotEmpty()
}
