package khaled.benzo.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TorrentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTorrent(torrent: TorrentEntity)

    @Update
    suspend fun updateTorrent(torrent: TorrentEntity)

    @Delete
    suspend fun deleteTorrent(torrent: TorrentEntity)

    @Query("SELECT * FROM torrents WHERE infoHash = :infoHash")
    suspend fun getTorrentByHash(infoHash: String): TorrentEntity?

    @Query("SELECT * FROM torrents")
    fun getAllTorrentsFlow(): Flow<List<TorrentEntity>>
}
