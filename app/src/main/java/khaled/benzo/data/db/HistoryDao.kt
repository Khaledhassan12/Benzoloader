package khaled.benzo.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface HistoryDao {
    @Insert
    suspend fun insert(entry: HistoryEntity): Long

    @Query("SELECT * FROM history ORDER BY visited_at DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<HistoryEntity>

    @Query("SELECT url FROM history ORDER BY visited_at DESC LIMIT 1")
    suspend fun lastUrl(): String?

    @Query("DELETE FROM history")
    suspend fun clear(): Int

    /** Keeps the table bounded: removes everything except the newest [keep] rows. */
    @Query("DELETE FROM history WHERE id NOT IN (SELECT id FROM history ORDER BY visited_at DESC LIMIT :keep)")
    suspend fun trimTo(keep: Int): Int
}
