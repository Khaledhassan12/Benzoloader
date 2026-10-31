package khaled.benzo.data.model

data class TorrentItem(
    val infoHash: String,
    val name: String,
    val totalSize: Long,
    val downloadedSize: Long,
    val uploadSize: Long,
    val downloadSpeed: Long,
    val uploadSpeed: Long,
    val seeds: Int,
    val peers: Int,
    val state: String,
    val savePath: String,
    val files: List<TorrentFileEntry> = emptyList()
)

data class TorrentFileEntry(
    val index: Int,
    val path: String,
    val size: Long,
    var isSelected: Boolean = true,
    var priority: Int = 1 // 0: Skip, 1: Normal, 7: High
)
