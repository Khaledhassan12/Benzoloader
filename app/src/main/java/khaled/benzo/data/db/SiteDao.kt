package khaled.benzo.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSite(site: SiteEntity): Long

    @Update
    suspend fun updateSite(site: SiteEntity)

    @Delete
    suspend fun deleteSite(site: SiteEntity)

    @Query("SELECT * FROM sites WHERE domain = :domain LIMIT 1")
    suspend fun getSiteByDomain(domain: String): SiteEntity?

    @Query("SELECT * FROM sites ORDER BY domain ASC")
    suspend fun getAllSites(): List<SiteEntity>

    @Query("SELECT * FROM sites ORDER BY domain ASC")
    fun getAllSitesFlow(): Flow<List<SiteEntity>>

    @Query("UPDATE sites SET password = :newPassword, custom_cookies = :newCookies WHERE id = :id AND password IS :oldPassword AND custom_cookies IS :oldCookies")
    suspend fun swapSecrets(id: Long, oldPassword: String?, oldCookies: String?, newPassword: String?, newCookies: String?): Int

    @Query("DELETE FROM sites WHERE domain = :domain")
    suspend fun deleteByDomain(domain: String): Int
}
