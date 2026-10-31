package khaled.benzo.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity): Long

    @Update
    suspend fun updateDownload(download: DownloadEntity)

    @Delete
    suspend fun deleteDownload(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: Long): DownloadEntity?

    @Query("SELECT * FROM downloads ORDER BY created_date DESC")
    fun getAllDownloadsFlow(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY created_date DESC")
    fun getDownloadsByStatusFlow(status: Int): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE category = :category ORDER BY created_date DESC")
    fun getDownloadsByCategoryFlow(category: String): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN (0, 1, 2) ORDER BY created_date ASC")
    suspend fun getPendingAndActiveDownloads(): List<DownloadEntity>

    @Query("UPDATE downloads SET status = :status, speed = :speed WHERE id = :id")
    suspend fun updateStatusAndSpeed(id: Long, status: Int, speed: Long): Int

    @Query("UPDATE downloads SET downloaded_bytes = :downloadedBytes, speed = :speed WHERE id = :id")
    suspend fun updateProgressAndSpeed(id: Long, downloadedBytes: Long, speed: Long): Int

    @Query("UPDATE downloads SET status = :newStatus WHERE status = :oldStatus")
    suspend fun updateAllStatus(oldStatus: Int, newStatus: Int): Int
}
