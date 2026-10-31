package khaled.benzo.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import khaled.benzo.data.model.DownloadChunk

@Entity(
    tableName = "chunks",
    foreignKeys = [
        ForeignKey(
            entity = DownloadEntity::class,
            parentColumns = ["id"],
            childColumns = ["download_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("download_id")]
)
data class ChunkEntity(
    @PrimaryKey(autoGenerate = true) val chunkId: Long = 0,
    @ColumnInfo(name = "download_id") val downloadId: Long,
    @ColumnInfo(name = "part_index") val partIndex: Int,
    @ColumnInfo(name = "start_byte") val startByte: Long,
    @ColumnInfo(name = "current_byte") val currentByte: Long,
    @ColumnInfo(name = "end_byte") val endByte: Long,
    @ColumnInfo(name = "status") val status: Int = 0
) {
    fun toModel(): DownloadChunk {
        return DownloadChunk(
            chunkId = chunkId,
            downloadId = downloadId,
            partIndex = partIndex,
            startByte = startByte,
            currentByte = currentByte,
            endByte = endByte,
            status = status
        )
    }

    companion object {
        fun fromModel(model: DownloadChunk): ChunkEntity {
            return ChunkEntity(
                chunkId = model.chunkId,
                downloadId = model.downloadId,
                partIndex = model.partIndex,
                startByte = model.startByte,
                currentByte = model.currentByte,
                endByte = model.endByte,
                status = model.status
            )
        }
    }
}
