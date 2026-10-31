package khaled.benzo.`data`.db

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class BenzoDatabase_Impl : BenzoDatabase() {
  private val _downloadDao: Lazy<DownloadDao> = lazy {
    DownloadDao_Impl(this)
  }

  private val _chunkDao: Lazy<ChunkDao> = lazy {
    ChunkDao_Impl(this)
  }

  private val _siteDao: Lazy<SiteDao> = lazy {
    SiteDao_Impl(this)
  }

  private val _torrentDao: Lazy<TorrentDao> = lazy {
    TorrentDao_Impl(this)
  }

  private val _bookmarkDao: Lazy<BookmarkDao> = lazy {
    BookmarkDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(1, "ebec5c3b22cadc97c1e3a99415599d32", "6079c89fcdd4f8cf014badf10fe19cef") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `downloads` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `url` TEXT NOT NULL, `file_name` TEXT NOT NULL, `save_path` TEXT NOT NULL, `total_bytes` INTEGER NOT NULL, `downloaded_bytes` INTEGER NOT NULL, `status` INTEGER NOT NULL, `speed` INTEGER NOT NULL, `threads_count` INTEGER NOT NULL, `created_date` INTEGER NOT NULL, `completed_date` INTEGER NOT NULL, `mime_type` TEXT, `user_agent` TEXT, `cookies` TEXT, `referer` TEXT, `headers` TEXT, `category` TEXT NOT NULL, `error_message` TEXT, `is_torrent` INTEGER NOT NULL, `torrent_info_hash` TEXT, `is_wifi_only` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `chunks` (`chunkId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `download_id` INTEGER NOT NULL, `part_index` INTEGER NOT NULL, `start_byte` INTEGER NOT NULL, `current_byte` INTEGER NOT NULL, `end_byte` INTEGER NOT NULL, `status` INTEGER NOT NULL, FOREIGN KEY(`download_id`) REFERENCES `downloads`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_chunks_download_id` ON `chunks` (`download_id`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `sites` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `domain` TEXT NOT NULL, `username` TEXT, `password` TEXT, `user_agent` TEXT, `custom_cookies` TEXT)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `torrents` (`infoHash` TEXT NOT NULL, `name` TEXT NOT NULL, `total_size` INTEGER NOT NULL, `downloaded_size` INTEGER NOT NULL, `upload_size` INTEGER NOT NULL, `state` TEXT NOT NULL, `save_path` TEXT NOT NULL, `torrent_file_path` TEXT, PRIMARY KEY(`infoHash`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `bookmarks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `url` TEXT NOT NULL, `created_at` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'ebec5c3b22cadc97c1e3a99415599d32')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `downloads`")
        connection.execSQL("DROP TABLE IF EXISTS `chunks`")
        connection.execSQL("DROP TABLE IF EXISTS `sites`")
        connection.execSQL("DROP TABLE IF EXISTS `torrents`")
        connection.execSQL("DROP TABLE IF EXISTS `bookmarks`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        connection.execSQL("PRAGMA foreign_keys = ON")
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
        val _columnsDownloads: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsDownloads.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloads.put("url", TableInfo.Column("url", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloads.put("file_name", TableInfo.Column("file_name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloads.put("save_path", TableInfo.Column("save_path", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloads.put("total_bytes", TableInfo.Column("total_bytes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloads.put("downloaded_bytes", TableInfo.Column("downloaded_bytes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloads.put("status", TableInfo.Column("status", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloads.put("speed", TableInfo.Column("speed", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloads.put("threads_count", TableInfo.Column("threads_count", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloads.put("created_date", TableInfo.Column("created_date", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloads.put("completed_date", TableInfo.Column("completed_date", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloads.put("mime_type", TableInfo.Column("mime_type", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloads.put("user_agent", TableInfo.Column("user_agent", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloads.put("cookies", TableInfo.Column("cookies", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloads.put("referer", TableInfo.Column("referer", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloads.put("headers", TableInfo.Column("headers", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloads.put("category", TableInfo.Column("category", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloads.put("error_message", TableInfo.Column("error_message", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloads.put("is_torrent", TableInfo.Column("is_torrent", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloads.put("torrent_info_hash", TableInfo.Column("torrent_info_hash", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDownloads.put("is_wifi_only", TableInfo.Column("is_wifi_only", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysDownloads: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesDownloads: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoDownloads: TableInfo = TableInfo("downloads", _columnsDownloads, _foreignKeysDownloads, _indicesDownloads)
        val _existingDownloads: TableInfo = read(connection, "downloads")
        if (!_infoDownloads.equals(_existingDownloads)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |downloads(khaled.benzo.data.db.DownloadEntity).
              | Expected:
              |""".trimMargin() + _infoDownloads + """
              |
              | Found:
              |""".trimMargin() + _existingDownloads)
        }
        val _columnsChunks: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsChunks.put("chunkId", TableInfo.Column("chunkId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsChunks.put("download_id", TableInfo.Column("download_id", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsChunks.put("part_index", TableInfo.Column("part_index", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsChunks.put("start_byte", TableInfo.Column("start_byte", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsChunks.put("current_byte", TableInfo.Column("current_byte", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsChunks.put("end_byte", TableInfo.Column("end_byte", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsChunks.put("status", TableInfo.Column("status", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysChunks: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysChunks.add(TableInfo.ForeignKey("downloads", "CASCADE", "NO ACTION", listOf("download_id"), listOf("id")))
        val _indicesChunks: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesChunks.add(TableInfo.Index("index_chunks_download_id", false, listOf("download_id"), listOf("ASC")))
        val _infoChunks: TableInfo = TableInfo("chunks", _columnsChunks, _foreignKeysChunks, _indicesChunks)
        val _existingChunks: TableInfo = read(connection, "chunks")
        if (!_infoChunks.equals(_existingChunks)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |chunks(khaled.benzo.data.db.ChunkEntity).
              | Expected:
              |""".trimMargin() + _infoChunks + """
              |
              | Found:
              |""".trimMargin() + _existingChunks)
        }
        val _columnsSites: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsSites.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSites.put("domain", TableInfo.Column("domain", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSites.put("username", TableInfo.Column("username", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSites.put("password", TableInfo.Column("password", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSites.put("user_agent", TableInfo.Column("user_agent", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSites.put("custom_cookies", TableInfo.Column("custom_cookies", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysSites: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesSites: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoSites: TableInfo = TableInfo("sites", _columnsSites, _foreignKeysSites, _indicesSites)
        val _existingSites: TableInfo = read(connection, "sites")
        if (!_infoSites.equals(_existingSites)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |sites(khaled.benzo.data.db.SiteEntity).
              | Expected:
              |""".trimMargin() + _infoSites + """
              |
              | Found:
              |""".trimMargin() + _existingSites)
        }
        val _columnsTorrents: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsTorrents.put("infoHash", TableInfo.Column("infoHash", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTorrents.put("name", TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTorrents.put("total_size", TableInfo.Column("total_size", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTorrents.put("downloaded_size", TableInfo.Column("downloaded_size", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTorrents.put("upload_size", TableInfo.Column("upload_size", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTorrents.put("state", TableInfo.Column("state", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTorrents.put("save_path", TableInfo.Column("save_path", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTorrents.put("torrent_file_path", TableInfo.Column("torrent_file_path", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysTorrents: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesTorrents: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoTorrents: TableInfo = TableInfo("torrents", _columnsTorrents, _foreignKeysTorrents, _indicesTorrents)
        val _existingTorrents: TableInfo = read(connection, "torrents")
        if (!_infoTorrents.equals(_existingTorrents)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |torrents(khaled.benzo.data.db.TorrentEntity).
              | Expected:
              |""".trimMargin() + _infoTorrents + """
              |
              | Found:
              |""".trimMargin() + _existingTorrents)
        }
        val _columnsBookmarks: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsBookmarks.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBookmarks.put("title", TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBookmarks.put("url", TableInfo.Column("url", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBookmarks.put("created_at", TableInfo.Column("created_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysBookmarks: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesBookmarks: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoBookmarks: TableInfo = TableInfo("bookmarks", _columnsBookmarks, _foreignKeysBookmarks, _indicesBookmarks)
        val _existingBookmarks: TableInfo = read(connection, "bookmarks")
        if (!_infoBookmarks.equals(_existingBookmarks)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |bookmarks(khaled.benzo.data.db.BookmarkEntity).
              | Expected:
              |""".trimMargin() + _infoBookmarks + """
              |
              | Found:
              |""".trimMargin() + _existingBookmarks)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "downloads", "chunks", "sites", "torrents", "bookmarks")
  }

  public override fun clearAllTables() {
    super.performClear(true, "downloads", "chunks", "sites", "torrents", "bookmarks")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(DownloadDao::class, DownloadDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(ChunkDao::class, ChunkDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(SiteDao::class, SiteDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(TorrentDao::class, TorrentDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(BookmarkDao::class, BookmarkDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>): List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun downloadDao(): DownloadDao = _downloadDao.value

  public override fun chunkDao(): ChunkDao = _chunkDao.value

  public override fun siteDao(): SiteDao = _siteDao.value

  public override fun torrentDao(): TorrentDao = _torrentDao.value

  public override fun bookmarkDao(): BookmarkDao = _bookmarkDao.value
}
