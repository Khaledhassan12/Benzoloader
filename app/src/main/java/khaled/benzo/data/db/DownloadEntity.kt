package khaled.benzo.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import khaled.benzo.data.model.DownloadItem
import khaled.benzo.data.model.DownloadStatus
import khaled.benzo.data.model.FileCategory

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "save_path") val savePath: String,
    @ColumnInfo(name = "total_bytes") val totalBytes: Long = -1L,
    @ColumnInfo(name = "downloaded_bytes") val downloadedBytes: Long = 0L,
    @ColumnInfo(name = "status") val status: Int = 0,
    @ColumnInfo(name = "speed") val speed: Long = 0L,
    @ColumnInfo(name = "threads_count") val threadsCount: Int = 4,
    @ColumnInfo(name = "created_date") val createdDate: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "completed_date") val completedDate: Long = 0L,
    @ColumnInfo(name = "mime_type") val mimeType: String? = null,
    @ColumnInfo(name = "user_agent") val userAgent: String? = null,
    @ColumnInfo(name = "cookies") val cookies: String? = null,
    @ColumnInfo(name = "referer") val referer: String? = null,
    @ColumnInfo(name = "headers") val headers: String? = null,
    @ColumnInfo(name = "category") val category: String = "document",
    @ColumnInfo(name = "error_message") val errorMessage: String? = null,
    @ColumnInfo(name = "is_torrent") val isTorrent: Boolean = false,
    @ColumnInfo(name = "torrent_info_hash") val torrentInfoHash: String? = null,
    @ColumnInfo(name = "is_wifi_only") val isWifiOnly: Boolean = false,
    @ColumnInfo(name = "speed_limit_kbps", defaultValue = "0") val speedLimitKBps: Int = 0,
    @ColumnInfo(name = "expected_checksum") val expectedChecksum: String? = null,
    @ColumnInfo(name = "list_order", defaultValue = "0") val listOrder: Long = 0L,
    @ColumnInfo(name = "after_finish", defaultValue = "0") val afterFinish: Int = 0,
    /** Milliseconds this download spent actually transferring (summed over runs). Used for the finished-row average speed and the elapsed time. */
    @ColumnInfo(name = "active_ms", defaultValue = "0") val activeMs: Long = 0L
) {
    fun toModel(): DownloadItem {
        val ck = khaled.benzo.core.security.Secrets.read(cookies)
        val hd = khaled.benzo.core.security.Secrets.read(headers)
        return DownloadItem(
            id = id,
            url = url,
            fileName = fileName,
            savePath = savePath,
            totalBytes = totalBytes,
            downloadedBytes = downloadedBytes,
            status = DownloadStatus.fromCode(status),
            speed = speed,
            threadsCount = threadsCount,
            createdDate = createdDate,
            completedDate = completedDate,
            mimeType = mimeType,
            userAgent = userAgent,
            cookies = (ck as? khaled.benzo.core.security.SecretRead.Ok)?.value,
            rawCookies = (ck as? khaled.benzo.core.security.SecretRead.Unreadable)?.raw,
            referer = referer,
            headersJson = (hd as? khaled.benzo.core.security.SecretRead.Ok)?.value,
            rawHeaders = (hd as? khaled.benzo.core.security.SecretRead.Unreadable)?.raw,
            category = FileCategory.entries.firstOrNull { it.key == category } ?: FileCategory.DOCUMENT,
            errorMessage = errorMessage,
            isTorrent = isTorrent,
            torrentInfoHash = torrentInfoHash,
            isWifiOnly = isWifiOnly,
            speedLimitKBps = speedLimitKBps,
            expectedChecksum = expectedChecksum,
            listOrder = listOrder,
            afterFinish = afterFinish,
            activeMs = activeMs
        )
    }

    companion object {
        fun fromModel(model: DownloadItem): DownloadEntity {
            return DownloadEntity(
                id = model.id,
                url = model.url,
                fileName = model.fileName,
                savePath = model.savePath,
                totalBytes = model.totalBytes,
                downloadedBytes = model.downloadedBytes,
                status = model.status.code,
                speed = model.speed,
                threadsCount = model.threadsCount,
                createdDate = model.createdDate,
                completedDate = model.completedDate,
                mimeType = model.mimeType,
                userAgent = model.userAgent,
                cookies = khaled.benzo.core.security.Secrets.sealOrKeep(model.cookies, model.rawCookies),
                referer = model.referer,
                headers = khaled.benzo.core.security.Secrets.sealOrKeep(model.headersJson, model.rawHeaders),
                category = model.category.key,
                errorMessage = model.errorMessage,
                isTorrent = model.isTorrent,
                torrentInfoHash = model.torrentInfoHash,
                isWifiOnly = model.isWifiOnly,
                speedLimitKBps = model.speedLimitKBps,
                expectedChecksum = model.expectedChecksum,
                listOrder = model.listOrder,
                afterFinish = model.afterFinish,
                activeMs = model.activeMs
            )
        }
    }
}
