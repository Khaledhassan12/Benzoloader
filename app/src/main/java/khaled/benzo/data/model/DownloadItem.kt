package khaled.benzo.data.model

data class DownloadItem(
    val id: Long = 0,
    val url: String,
    val fileName: String,
    val savePath: String,
    val totalBytes: Long = -1L,
    val downloadedBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val speed: Long = 0L, // bytes per second
    val threadsCount: Int = 4,
    val createdDate: Long = System.currentTimeMillis(),
    val completedDate: Long = 0L,
    val mimeType: String? = null,
    val userAgent: String? = null,
    val cookies: String? = null,
    val referer: String? = null,
    val headersJson: String? = null,
    val category: FileCategory = FileCategory.DOCUMENT,
    val errorMessage: String? = null,
    val isTorrent: Boolean = false,
    val torrentInfoHash: String? = null,
    val isWifiOnly: Boolean = false
) {
    val parts: Int
        get() = threadsCount

    val progressPercent: Int
        get() = if (totalBytes > 0L) {
            ((downloadedBytes.toDouble() / totalBytes.toDouble()) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }

    val isIndeterminate: Boolean
        get() = totalBytes <= 0L && status == DownloadStatus.RUNNING
}
