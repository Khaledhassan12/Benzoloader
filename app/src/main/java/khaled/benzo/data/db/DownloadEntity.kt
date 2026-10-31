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
    @ColumnInfo(name = "is_wifi_only") val isWifiOnly: Boolean = false
) {
    fun toModel(): DownloadItem {
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
            cookies = cookies,
            referer = referer,
            headersJson = headers,
            category = FileCategory.entries.firstOrNull { it.key == category } ?: FileCategory.DOCUMENT,
            errorMessage = errorMessage,
            isTorrent = isTorrent,
            torrentInfoHash = torrentInfoHash,
            isWifiOnly = isWifiOnly
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
                cookies = model.cookies,
                referer = model.referer,
                headers = model.headersJson,
                category = model.category.key,
                errorMessage = model.errorMessage,
                isTorrent = model.isTorrent,
                torrentInfoHash = model.torrentInfoHash,
                isWifiOnly = model.isWifiOnly
            )
        }
    }
}
