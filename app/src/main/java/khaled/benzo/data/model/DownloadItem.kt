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
    val isWifiOnly: Boolean = false,
    /** Stored cookies/headers that could not be decrypted, kept as stored so they are never overwritten with nothing. */
    val rawCookies: String? = null,
    val rawHeaders: String? = null,
    /** Limit for this download alone in KB/s. 0 = use the global limit only. */
    val speedLimitKBps: Int = 0,
    /** Expected file checksum as hex (MD5, SHA-1 or SHA-256 by length). Null = no check. */
    val expectedChecksum: String? = null,
    /** Hand-set position for the Order sort. 0 = never moved: the item sorts by its id, as before. */
    val listOrder: Long = 0L,
    /** After finishing, for this download: 0 = follow the general setting, 1 = do nothing, 2 = exit when it finishes. INFERRED meaning (the reference Editor shows an "After finishing" dropdown, value "exit"; the choices were not seen). */
    val afterFinish: Int = 0,
    /** Milliseconds spent actually transferring, summed over runs. 0 for downloads made before this was recorded. */
    val activeMs: Long = 0L
) {
    /** What the Order sort compares. Moved items carry their own key; the rest keep their id. */
    val orderKey: Long get() = if (listOrder > 0L) listOrder else id

    /** True when saved cookies or headers cannot be read (security key changed). The user must enter them again. */
    val needsReentry: Boolean
        get() = rawCookies != null || rawHeaders != null

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
