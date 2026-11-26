package khaled.benzo.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DownloadEntity::class,
        ChunkEntity::class,
        SiteEntity::class,
        TorrentEntity::class,
        BookmarkEntity::class,
        HistoryEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class BenzoDatabase : RoomDatabase() {

    abstract fun downloadDao(): DownloadDao
    abstract fun chunkDao(): ChunkDao
    abstract fun siteDao(): SiteDao
    abstract fun torrentDao(): TorrentDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: BenzoDatabase? = null

        fun getInstance(context: Context): BenzoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BenzoDatabase::class.java,
                    "benzo_database.db"
                )
                    .addMigrations(*Migrations.ALL)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
