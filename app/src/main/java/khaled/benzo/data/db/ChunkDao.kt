package khaled.benzo.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ChunkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<ChunkEntity>)

    @Update
    suspend fun updateChunk(chunk: ChunkEntity)

    @Query("SELECT * FROM chunks WHERE download_id = :downloadId ORDER BY part_index ASC")
    suspend fun getChunksForDownload(downloadId: Long): List<ChunkEntity>

    @Query("UPDATE chunks SET current_byte = :currentByte, status = :status WHERE chunkId = :chunkId")
    suspend fun updateChunkProgress(chunkId: Long, currentByte: Long, status: Int): Int

    @Query("DELETE FROM chunks WHERE download_id = :downloadId")
    suspend fun deleteChunksForDownload(downloadId: Long): Int
}
