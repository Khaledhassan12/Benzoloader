package khaled.benzo.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "torrents")
data class TorrentEntity(
    @PrimaryKey val infoHash: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "total_size") val totalSize: Long,
    @ColumnInfo(name = "downloaded_size") val downloadedSize: Long,
    @ColumnInfo(name = "upload_size") val uploadSize: Long,
    @ColumnInfo(name = "state") val state: String,
    @ColumnInfo(name = "save_path") val savePath: String,
    @ColumnInfo(name = "torrent_file_path") val torrentFilePath: String? = null
)
