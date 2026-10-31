package khaled.benzo.`data`.db

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.getTotalChangedRows
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class DownloadDao_Impl(
  __db: RoomDatabase,
) : DownloadDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfDownloadEntity: EntityInsertAdapter<DownloadEntity>

  private val __deleteAdapterOfDownloadEntity: EntityDeleteOrUpdateAdapter<DownloadEntity>

  private val __updateAdapterOfDownloadEntity: EntityDeleteOrUpdateAdapter<DownloadEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfDownloadEntity = object : EntityInsertAdapter<DownloadEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `downloads` (`id`,`url`,`file_name`,`save_path`,`total_bytes`,`downloaded_bytes`,`status`,`speed`,`threads_count`,`created_date`,`completed_date`,`mime_type`,`user_agent`,`cookies`,`referer`,`headers`,`category`,`error_message`,`is_torrent`,`torrent_info_hash`,`is_wifi_only`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: DownloadEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.url)
        statement.bindText(3, entity.fileName)
        statement.bindText(4, entity.savePath)
        statement.bindLong(5, entity.totalBytes)
        statement.bindLong(6, entity.downloadedBytes)
        statement.bindLong(7, entity.status.toLong())
        statement.bindLong(8, entity.speed)
        statement.bindLong(9, entity.threadsCount.toLong())
        statement.bindLong(10, entity.createdDate)
        statement.bindLong(11, entity.completedDate)
        val _tmpMimeType: String? = entity.mimeType
        if (_tmpMimeType == null) {
          statement.bindNull(12)
        } else {
          statement.bindText(12, _tmpMimeType)
        }
        val _tmpUserAgent: String? = entity.userAgent
        if (_tmpUserAgent == null) {
          statement.bindNull(13)
        } else {
          statement.bindText(13, _tmpUserAgent)
        }
        val _tmpCookies: String? = entity.cookies
        if (_tmpCookies == null) {
          statement.bindNull(14)
        } else {
          statement.bindText(14, _tmpCookies)
        }
        val _tmpReferer: String? = entity.referer
        if (_tmpReferer == null) {
          statement.bindNull(15)
        } else {
          statement.bindText(15, _tmpReferer)
        }
        val _tmpHeaders: String? = entity.headers
        if (_tmpHeaders == null) {
          statement.bindNull(16)
        } else {
          statement.bindText(16, _tmpHeaders)
        }
        statement.bindText(17, entity.category)
        val _tmpErrorMessage: String? = entity.errorMessage
        if (_tmpErrorMessage == null) {
          statement.bindNull(18)
        } else {
          statement.bindText(18, _tmpErrorMessage)
        }
        val _tmp: Int = if (entity.isTorrent) 1 else 0
        statement.bindLong(19, _tmp.toLong())
        val _tmpTorrentInfoHash: String? = entity.torrentInfoHash
        if (_tmpTorrentInfoHash == null) {
          statement.bindNull(20)
        } else {
          statement.bindText(20, _tmpTorrentInfoHash)
        }
        val _tmp_1: Int = if (entity.isWifiOnly) 1 else 0
        statement.bindLong(21, _tmp_1.toLong())
      }
    }
    this.__deleteAdapterOfDownloadEntity = object : EntityDeleteOrUpdateAdapter<DownloadEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `downloads` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: DownloadEntity) {
        statement.bindLong(1, entity.id)
      }
    }
    this.__updateAdapterOfDownloadEntity = object : EntityDeleteOrUpdateAdapter<DownloadEntity>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `downloads` SET `id` = ?,`url` = ?,`file_name` = ?,`save_path` = ?,`total_bytes` = ?,`downloaded_bytes` = ?,`status` = ?,`speed` = ?,`threads_count` = ?,`created_date` = ?,`completed_date` = ?,`mime_type` = ?,`user_agent` = ?,`cookies` = ?,`referer` = ?,`headers` = ?,`category` = ?,`error_message` = ?,`is_torrent` = ?,`torrent_info_hash` = ?,`is_wifi_only` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: DownloadEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.url)
        statement.bindText(3, entity.fileName)
        statement.bindText(4, entity.savePath)
        statement.bindLong(5, entity.totalBytes)
        statement.bindLong(6, entity.downloadedBytes)
        statement.bindLong(7, entity.status.toLong())
        statement.bindLong(8, entity.speed)
        statement.bindLong(9, entity.threadsCount.toLong())
        statement.bindLong(10, entity.createdDate)
        statement.bindLong(11, entity.completedDate)
        val _tmpMimeType: String? = entity.mimeType
        if (_tmpMimeType == null) {
          statement.bindNull(12)
        } else {
          statement.bindText(12, _tmpMimeType)
        }
        val _tmpUserAgent: String? = entity.userAgent
        if (_tmpUserAgent == null) {
          statement.bindNull(13)
        } else {
          statement.bindText(13, _tmpUserAgent)
        }
        val _tmpCookies: String? = entity.cookies
        if (_tmpCookies == null) {
          statement.bindNull(14)
        } else {
          statement.bindText(14, _tmpCookies)
        }
        val _tmpReferer: String? = entity.referer
        if (_tmpReferer == null) {
          statement.bindNull(15)
        } else {
          statement.bindText(15, _tmpReferer)
        }
        val _tmpHeaders: String? = entity.headers
        if (_tmpHeaders == null) {
          statement.bindNull(16)
        } else {
          statement.bindText(16, _tmpHeaders)
        }
        statement.bindText(17, entity.category)
        val _tmpErrorMessage: String? = entity.errorMessage
        if (_tmpErrorMessage == null) {
          statement.bindNull(18)
        } else {
          statement.bindText(18, _tmpErrorMessage)
        }
        val _tmp: Int = if (entity.isTorrent) 1 else 0
        statement.bindLong(19, _tmp.toLong())
        val _tmpTorrentInfoHash: String? = entity.torrentInfoHash
        if (_tmpTorrentInfoHash == null) {
          statement.bindNull(20)
        } else {
          statement.bindText(20, _tmpTorrentInfoHash)
        }
        val _tmp_1: Int = if (entity.isWifiOnly) 1 else 0
        statement.bindLong(21, _tmp_1.toLong())
        statement.bindLong(22, entity.id)
      }
    }
  }

  public override suspend fun insertDownload(download: DownloadEntity): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfDownloadEntity.insertAndReturnId(_connection, download)
    _result
  }

  public override suspend fun deleteDownload(download: DownloadEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfDownloadEntity.handle(_connection, download)
  }

  public override suspend fun updateDownload(download: DownloadEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfDownloadEntity.handle(_connection, download)
  }

  public override suspend fun getDownloadById(id: Long): DownloadEntity? {
    val _sql: String = "SELECT * FROM downloads WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUrl: Int = getColumnIndexOrThrow(_stmt, "url")
        val _columnIndexOfFileName: Int = getColumnIndexOrThrow(_stmt, "file_name")
        val _columnIndexOfSavePath: Int = getColumnIndexOrThrow(_stmt, "save_path")
        val _columnIndexOfTotalBytes: Int = getColumnIndexOrThrow(_stmt, "total_bytes")
        val _columnIndexOfDownloadedBytes: Int = getColumnIndexOrThrow(_stmt, "downloaded_bytes")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfSpeed: Int = getColumnIndexOrThrow(_stmt, "speed")
        val _columnIndexOfThreadsCount: Int = getColumnIndexOrThrow(_stmt, "threads_count")
        val _columnIndexOfCreatedDate: Int = getColumnIndexOrThrow(_stmt, "created_date")
        val _columnIndexOfCompletedDate: Int = getColumnIndexOrThrow(_stmt, "completed_date")
        val _columnIndexOfMimeType: Int = getColumnIndexOrThrow(_stmt, "mime_type")
        val _columnIndexOfUserAgent: Int = getColumnIndexOrThrow(_stmt, "user_agent")
        val _columnIndexOfCookies: Int = getColumnIndexOrThrow(_stmt, "cookies")
        val _columnIndexOfReferer: Int = getColumnIndexOrThrow(_stmt, "referer")
        val _columnIndexOfHeaders: Int = getColumnIndexOrThrow(_stmt, "headers")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfErrorMessage: Int = getColumnIndexOrThrow(_stmt, "error_message")
        val _columnIndexOfIsTorrent: Int = getColumnIndexOrThrow(_stmt, "is_torrent")
        val _columnIndexOfTorrentInfoHash: Int = getColumnIndexOrThrow(_stmt, "torrent_info_hash")
        val _columnIndexOfIsWifiOnly: Int = getColumnIndexOrThrow(_stmt, "is_wifi_only")
        val _result: DownloadEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpUrl: String
          _tmpUrl = _stmt.getText(_columnIndexOfUrl)
          val _tmpFileName: String
          _tmpFileName = _stmt.getText(_columnIndexOfFileName)
          val _tmpSavePath: String
          _tmpSavePath = _stmt.getText(_columnIndexOfSavePath)
          val _tmpTotalBytes: Long
          _tmpTotalBytes = _stmt.getLong(_columnIndexOfTotalBytes)
          val _tmpDownloadedBytes: Long
          _tmpDownloadedBytes = _stmt.getLong(_columnIndexOfDownloadedBytes)
          val _tmpStatus: Int
          _tmpStatus = _stmt.getLong(_columnIndexOfStatus).toInt()
          val _tmpSpeed: Long
          _tmpSpeed = _stmt.getLong(_columnIndexOfSpeed)
          val _tmpThreadsCount: Int
          _tmpThreadsCount = _stmt.getLong(_columnIndexOfThreadsCount).toInt()
          val _tmpCreatedDate: Long
          _tmpCreatedDate = _stmt.getLong(_columnIndexOfCreatedDate)
          val _tmpCompletedDate: Long
          _tmpCompletedDate = _stmt.getLong(_columnIndexOfCompletedDate)
          val _tmpMimeType: String?
          if (_stmt.isNull(_columnIndexOfMimeType)) {
            _tmpMimeType = null
          } else {
            _tmpMimeType = _stmt.getText(_columnIndexOfMimeType)
          }
          val _tmpUserAgent: String?
          if (_stmt.isNull(_columnIndexOfUserAgent)) {
            _tmpUserAgent = null
          } else {
            _tmpUserAgent = _stmt.getText(_columnIndexOfUserAgent)
          }
          val _tmpCookies: String?
          if (_stmt.isNull(_columnIndexOfCookies)) {
            _tmpCookies = null
          } else {
            _tmpCookies = _stmt.getText(_columnIndexOfCookies)
          }
          val _tmpReferer: String?
          if (_stmt.isNull(_columnIndexOfReferer)) {
            _tmpReferer = null
          } else {
            _tmpReferer = _stmt.getText(_columnIndexOfReferer)
          }
          val _tmpHeaders: String?
          if (_stmt.isNull(_columnIndexOfHeaders)) {
            _tmpHeaders = null
          } else {
            _tmpHeaders = _stmt.getText(_columnIndexOfHeaders)
          }
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpErrorMessage: String?
          if (_stmt.isNull(_columnIndexOfErrorMessage)) {
            _tmpErrorMessage = null
          } else {
            _tmpErrorMessage = _stmt.getText(_columnIndexOfErrorMessage)
          }
          val _tmpIsTorrent: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsTorrent).toInt()
          _tmpIsTorrent = _tmp != 0
          val _tmpTorrentInfoHash: String?
          if (_stmt.isNull(_columnIndexOfTorrentInfoHash)) {
            _tmpTorrentInfoHash = null
          } else {
            _tmpTorrentInfoHash = _stmt.getText(_columnIndexOfTorrentInfoHash)
          }
          val _tmpIsWifiOnly: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsWifiOnly).toInt()
          _tmpIsWifiOnly = _tmp_1 != 0
          _result = DownloadEntity(_tmpId,_tmpUrl,_tmpFileName,_tmpSavePath,_tmpTotalBytes,_tmpDownloadedBytes,_tmpStatus,_tmpSpeed,_tmpThreadsCount,_tmpCreatedDate,_tmpCompletedDate,_tmpMimeType,_tmpUserAgent,_tmpCookies,_tmpReferer,_tmpHeaders,_tmpCategory,_tmpErrorMessage,_tmpIsTorrent,_tmpTorrentInfoHash,_tmpIsWifiOnly)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getAllDownloadsFlow(): Flow<List<DownloadEntity>> {
    val _sql: String = "SELECT * FROM downloads ORDER BY created_date DESC"
    return createFlow(__db, false, arrayOf("downloads")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUrl: Int = getColumnIndexOrThrow(_stmt, "url")
        val _columnIndexOfFileName: Int = getColumnIndexOrThrow(_stmt, "file_name")
        val _columnIndexOfSavePath: Int = getColumnIndexOrThrow(_stmt, "save_path")
        val _columnIndexOfTotalBytes: Int = getColumnIndexOrThrow(_stmt, "total_bytes")
        val _columnIndexOfDownloadedBytes: Int = getColumnIndexOrThrow(_stmt, "downloaded_bytes")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfSpeed: Int = getColumnIndexOrThrow(_stmt, "speed")
        val _columnIndexOfThreadsCount: Int = getColumnIndexOrThrow(_stmt, "threads_count")
        val _columnIndexOfCreatedDate: Int = getColumnIndexOrThrow(_stmt, "created_date")
        val _columnIndexOfCompletedDate: Int = getColumnIndexOrThrow(_stmt, "completed_date")
        val _columnIndexOfMimeType: Int = getColumnIndexOrThrow(_stmt, "mime_type")
        val _columnIndexOfUserAgent: Int = getColumnIndexOrThrow(_stmt, "user_agent")
        val _columnIndexOfCookies: Int = getColumnIndexOrThrow(_stmt, "cookies")
        val _columnIndexOfReferer: Int = getColumnIndexOrThrow(_stmt, "referer")
        val _columnIndexOfHeaders: Int = getColumnIndexOrThrow(_stmt, "headers")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfErrorMessage: Int = getColumnIndexOrThrow(_stmt, "error_message")
        val _columnIndexOfIsTorrent: Int = getColumnIndexOrThrow(_stmt, "is_torrent")
        val _columnIndexOfTorrentInfoHash: Int = getColumnIndexOrThrow(_stmt, "torrent_info_hash")
        val _columnIndexOfIsWifiOnly: Int = getColumnIndexOrThrow(_stmt, "is_wifi_only")
        val _result: MutableList<DownloadEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: DownloadEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpUrl: String
          _tmpUrl = _stmt.getText(_columnIndexOfUrl)
          val _tmpFileName: String
          _tmpFileName = _stmt.getText(_columnIndexOfFileName)
          val _tmpSavePath: String
          _tmpSavePath = _stmt.getText(_columnIndexOfSavePath)
          val _tmpTotalBytes: Long
          _tmpTotalBytes = _stmt.getLong(_columnIndexOfTotalBytes)
          val _tmpDownloadedBytes: Long
          _tmpDownloadedBytes = _stmt.getLong(_columnIndexOfDownloadedBytes)
          val _tmpStatus: Int
          _tmpStatus = _stmt.getLong(_columnIndexOfStatus).toInt()
          val _tmpSpeed: Long
          _tmpSpeed = _stmt.getLong(_columnIndexOfSpeed)
          val _tmpThreadsCount: Int
          _tmpThreadsCount = _stmt.getLong(_columnIndexOfThreadsCount).toInt()
          val _tmpCreatedDate: Long
          _tmpCreatedDate = _stmt.getLong(_columnIndexOfCreatedDate)
          val _tmpCompletedDate: Long
          _tmpCompletedDate = _stmt.getLong(_columnIndexOfCompletedDate)
          val _tmpMimeType: String?
          if (_stmt.isNull(_columnIndexOfMimeType)) {
            _tmpMimeType = null
          } else {
            _tmpMimeType = _stmt.getText(_columnIndexOfMimeType)
          }
          val _tmpUserAgent: String?
          if (_stmt.isNull(_columnIndexOfUserAgent)) {
            _tmpUserAgent = null
          } else {
            _tmpUserAgent = _stmt.getText(_columnIndexOfUserAgent)
          }
          val _tmpCookies: String?
          if (_stmt.isNull(_columnIndexOfCookies)) {
            _tmpCookies = null
          } else {
            _tmpCookies = _stmt.getText(_columnIndexOfCookies)
          }
          val _tmpReferer: String?
          if (_stmt.isNull(_columnIndexOfReferer)) {
            _tmpReferer = null
          } else {
            _tmpReferer = _stmt.getText(_columnIndexOfReferer)
          }
          val _tmpHeaders: String?
          if (_stmt.isNull(_columnIndexOfHeaders)) {
            _tmpHeaders = null
          } else {
            _tmpHeaders = _stmt.getText(_columnIndexOfHeaders)
          }
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpErrorMessage: String?
          if (_stmt.isNull(_columnIndexOfErrorMessage)) {
            _tmpErrorMessage = null
          } else {
            _tmpErrorMessage = _stmt.getText(_columnIndexOfErrorMessage)
          }
          val _tmpIsTorrent: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsTorrent).toInt()
          _tmpIsTorrent = _tmp != 0
          val _tmpTorrentInfoHash: String?
          if (_stmt.isNull(_columnIndexOfTorrentInfoHash)) {
            _tmpTorrentInfoHash = null
          } else {
            _tmpTorrentInfoHash = _stmt.getText(_columnIndexOfTorrentInfoHash)
          }
          val _tmpIsWifiOnly: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsWifiOnly).toInt()
          _tmpIsWifiOnly = _tmp_1 != 0
          _item = DownloadEntity(_tmpId,_tmpUrl,_tmpFileName,_tmpSavePath,_tmpTotalBytes,_tmpDownloadedBytes,_tmpStatus,_tmpSpeed,_tmpThreadsCount,_tmpCreatedDate,_tmpCompletedDate,_tmpMimeType,_tmpUserAgent,_tmpCookies,_tmpReferer,_tmpHeaders,_tmpCategory,_tmpErrorMessage,_tmpIsTorrent,_tmpTorrentInfoHash,_tmpIsWifiOnly)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getDownloadsByStatusFlow(status: Int): Flow<List<DownloadEntity>> {
    val _sql: String = "SELECT * FROM downloads WHERE status = ? ORDER BY created_date DESC"
    return createFlow(__db, false, arrayOf("downloads")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, status.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUrl: Int = getColumnIndexOrThrow(_stmt, "url")
        val _columnIndexOfFileName: Int = getColumnIndexOrThrow(_stmt, "file_name")
        val _columnIndexOfSavePath: Int = getColumnIndexOrThrow(_stmt, "save_path")
        val _columnIndexOfTotalBytes: Int = getColumnIndexOrThrow(_stmt, "total_bytes")
        val _columnIndexOfDownloadedBytes: Int = getColumnIndexOrThrow(_stmt, "downloaded_bytes")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfSpeed: Int = getColumnIndexOrThrow(_stmt, "speed")
        val _columnIndexOfThreadsCount: Int = getColumnIndexOrThrow(_stmt, "threads_count")
        val _columnIndexOfCreatedDate: Int = getColumnIndexOrThrow(_stmt, "created_date")
        val _columnIndexOfCompletedDate: Int = getColumnIndexOrThrow(_stmt, "completed_date")
        val _columnIndexOfMimeType: Int = getColumnIndexOrThrow(_stmt, "mime_type")
        val _columnIndexOfUserAgent: Int = getColumnIndexOrThrow(_stmt, "user_agent")
        val _columnIndexOfCookies: Int = getColumnIndexOrThrow(_stmt, "cookies")
        val _columnIndexOfReferer: Int = getColumnIndexOrThrow(_stmt, "referer")
        val _columnIndexOfHeaders: Int = getColumnIndexOrThrow(_stmt, "headers")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfErrorMessage: Int = getColumnIndexOrThrow(_stmt, "error_message")
        val _columnIndexOfIsTorrent: Int = getColumnIndexOrThrow(_stmt, "is_torrent")
        val _columnIndexOfTorrentInfoHash: Int = getColumnIndexOrThrow(_stmt, "torrent_info_hash")
        val _columnIndexOfIsWifiOnly: Int = getColumnIndexOrThrow(_stmt, "is_wifi_only")
        val _result: MutableList<DownloadEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: DownloadEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpUrl: String
          _tmpUrl = _stmt.getText(_columnIndexOfUrl)
          val _tmpFileName: String
          _tmpFileName = _stmt.getText(_columnIndexOfFileName)
          val _tmpSavePath: String
          _tmpSavePath = _stmt.getText(_columnIndexOfSavePath)
          val _tmpTotalBytes: Long
          _tmpTotalBytes = _stmt.getLong(_columnIndexOfTotalBytes)
          val _tmpDownloadedBytes: Long
          _tmpDownloadedBytes = _stmt.getLong(_columnIndexOfDownloadedBytes)
          val _tmpStatus: Int
          _tmpStatus = _stmt.getLong(_columnIndexOfStatus).toInt()
          val _tmpSpeed: Long
          _tmpSpeed = _stmt.getLong(_columnIndexOfSpeed)
          val _tmpThreadsCount: Int
          _tmpThreadsCount = _stmt.getLong(_columnIndexOfThreadsCount).toInt()
          val _tmpCreatedDate: Long
          _tmpCreatedDate = _stmt.getLong(_columnIndexOfCreatedDate)
          val _tmpCompletedDate: Long
          _tmpCompletedDate = _stmt.getLong(_columnIndexOfCompletedDate)
          val _tmpMimeType: String?
          if (_stmt.isNull(_columnIndexOfMimeType)) {
            _tmpMimeType = null
          } else {
            _tmpMimeType = _stmt.getText(_columnIndexOfMimeType)
          }
          val _tmpUserAgent: String?
          if (_stmt.isNull(_columnIndexOfUserAgent)) {
            _tmpUserAgent = null
          } else {
            _tmpUserAgent = _stmt.getText(_columnIndexOfUserAgent)
          }
          val _tmpCookies: String?
          if (_stmt.isNull(_columnIndexOfCookies)) {
            _tmpCookies = null
          } else {
            _tmpCookies = _stmt.getText(_columnIndexOfCookies)
          }
          val _tmpReferer: String?
          if (_stmt.isNull(_columnIndexOfReferer)) {
            _tmpReferer = null
          } else {
            _tmpReferer = _stmt.getText(_columnIndexOfReferer)
          }
          val _tmpHeaders: String?
          if (_stmt.isNull(_columnIndexOfHeaders)) {
            _tmpHeaders = null
          } else {
            _tmpHeaders = _stmt.getText(_columnIndexOfHeaders)
          }
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpErrorMessage: String?
          if (_stmt.isNull(_columnIndexOfErrorMessage)) {
            _tmpErrorMessage = null
          } else {
            _tmpErrorMessage = _stmt.getText(_columnIndexOfErrorMessage)
          }
          val _tmpIsTorrent: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsTorrent).toInt()
          _tmpIsTorrent = _tmp != 0
          val _tmpTorrentInfoHash: String?
          if (_stmt.isNull(_columnIndexOfTorrentInfoHash)) {
            _tmpTorrentInfoHash = null
          } else {
            _tmpTorrentInfoHash = _stmt.getText(_columnIndexOfTorrentInfoHash)
          }
          val _tmpIsWifiOnly: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsWifiOnly).toInt()
          _tmpIsWifiOnly = _tmp_1 != 0
          _item = DownloadEntity(_tmpId,_tmpUrl,_tmpFileName,_tmpSavePath,_tmpTotalBytes,_tmpDownloadedBytes,_tmpStatus,_tmpSpeed,_tmpThreadsCount,_tmpCreatedDate,_tmpCompletedDate,_tmpMimeType,_tmpUserAgent,_tmpCookies,_tmpReferer,_tmpHeaders,_tmpCategory,_tmpErrorMessage,_tmpIsTorrent,_tmpTorrentInfoHash,_tmpIsWifiOnly)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getDownloadsByCategoryFlow(category: String): Flow<List<DownloadEntity>> {
    val _sql: String = "SELECT * FROM downloads WHERE category = ? ORDER BY created_date DESC"
    return createFlow(__db, false, arrayOf("downloads")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, category)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUrl: Int = getColumnIndexOrThrow(_stmt, "url")
        val _columnIndexOfFileName: Int = getColumnIndexOrThrow(_stmt, "file_name")
        val _columnIndexOfSavePath: Int = getColumnIndexOrThrow(_stmt, "save_path")
        val _columnIndexOfTotalBytes: Int = getColumnIndexOrThrow(_stmt, "total_bytes")
        val _columnIndexOfDownloadedBytes: Int = getColumnIndexOrThrow(_stmt, "downloaded_bytes")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfSpeed: Int = getColumnIndexOrThrow(_stmt, "speed")
        val _columnIndexOfThreadsCount: Int = getColumnIndexOrThrow(_stmt, "threads_count")
        val _columnIndexOfCreatedDate: Int = getColumnIndexOrThrow(_stmt, "created_date")
        val _columnIndexOfCompletedDate: Int = getColumnIndexOrThrow(_stmt, "completed_date")
        val _columnIndexOfMimeType: Int = getColumnIndexOrThrow(_stmt, "mime_type")
        val _columnIndexOfUserAgent: Int = getColumnIndexOrThrow(_stmt, "user_agent")
        val _columnIndexOfCookies: Int = getColumnIndexOrThrow(_stmt, "cookies")
        val _columnIndexOfReferer: Int = getColumnIndexOrThrow(_stmt, "referer")
        val _columnIndexOfHeaders: Int = getColumnIndexOrThrow(_stmt, "headers")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfErrorMessage: Int = getColumnIndexOrThrow(_stmt, "error_message")
        val _columnIndexOfIsTorrent: Int = getColumnIndexOrThrow(_stmt, "is_torrent")
        val _columnIndexOfTorrentInfoHash: Int = getColumnIndexOrThrow(_stmt, "torrent_info_hash")
        val _columnIndexOfIsWifiOnly: Int = getColumnIndexOrThrow(_stmt, "is_wifi_only")
        val _result: MutableList<DownloadEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: DownloadEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpUrl: String
          _tmpUrl = _stmt.getText(_columnIndexOfUrl)
          val _tmpFileName: String
          _tmpFileName = _stmt.getText(_columnIndexOfFileName)
          val _tmpSavePath: String
          _tmpSavePath = _stmt.getText(_columnIndexOfSavePath)
          val _tmpTotalBytes: Long
          _tmpTotalBytes = _stmt.getLong(_columnIndexOfTotalBytes)
          val _tmpDownloadedBytes: Long
          _tmpDownloadedBytes = _stmt.getLong(_columnIndexOfDownloadedBytes)
          val _tmpStatus: Int
          _tmpStatus = _stmt.getLong(_columnIndexOfStatus).toInt()
          val _tmpSpeed: Long
          _tmpSpeed = _stmt.getLong(_columnIndexOfSpeed)
          val _tmpThreadsCount: Int
          _tmpThreadsCount = _stmt.getLong(_columnIndexOfThreadsCount).toInt()
          val _tmpCreatedDate: Long
          _tmpCreatedDate = _stmt.getLong(_columnIndexOfCreatedDate)
          val _tmpCompletedDate: Long
          _tmpCompletedDate = _stmt.getLong(_columnIndexOfCompletedDate)
          val _tmpMimeType: String?
          if (_stmt.isNull(_columnIndexOfMimeType)) {
            _tmpMimeType = null
          } else {
            _tmpMimeType = _stmt.getText(_columnIndexOfMimeType)
          }
          val _tmpUserAgent: String?
          if (_stmt.isNull(_columnIndexOfUserAgent)) {
            _tmpUserAgent = null
          } else {
            _tmpUserAgent = _stmt.getText(_columnIndexOfUserAgent)
          }
          val _tmpCookies: String?
          if (_stmt.isNull(_columnIndexOfCookies)) {
            _tmpCookies = null
          } else {
            _tmpCookies = _stmt.getText(_columnIndexOfCookies)
          }
          val _tmpReferer: String?
          if (_stmt.isNull(_columnIndexOfReferer)) {
            _tmpReferer = null
          } else {
            _tmpReferer = _stmt.getText(_columnIndexOfReferer)
          }
          val _tmpHeaders: String?
          if (_stmt.isNull(_columnIndexOfHeaders)) {
            _tmpHeaders = null
          } else {
            _tmpHeaders = _stmt.getText(_columnIndexOfHeaders)
          }
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpErrorMessage: String?
          if (_stmt.isNull(_columnIndexOfErrorMessage)) {
            _tmpErrorMessage = null
          } else {
            _tmpErrorMessage = _stmt.getText(_columnIndexOfErrorMessage)
          }
          val _tmpIsTorrent: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsTorrent).toInt()
          _tmpIsTorrent = _tmp != 0
          val _tmpTorrentInfoHash: String?
          if (_stmt.isNull(_columnIndexOfTorrentInfoHash)) {
            _tmpTorrentInfoHash = null
          } else {
            _tmpTorrentInfoHash = _stmt.getText(_columnIndexOfTorrentInfoHash)
          }
          val _tmpIsWifiOnly: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsWifiOnly).toInt()
          _tmpIsWifiOnly = _tmp_1 != 0
          _item = DownloadEntity(_tmpId,_tmpUrl,_tmpFileName,_tmpSavePath,_tmpTotalBytes,_tmpDownloadedBytes,_tmpStatus,_tmpSpeed,_tmpThreadsCount,_tmpCreatedDate,_tmpCompletedDate,_tmpMimeType,_tmpUserAgent,_tmpCookies,_tmpReferer,_tmpHeaders,_tmpCategory,_tmpErrorMessage,_tmpIsTorrent,_tmpTorrentInfoHash,_tmpIsWifiOnly)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getPendingAndActiveDownloads(): List<DownloadEntity> {
    val _sql: String = "SELECT * FROM downloads WHERE status IN (0, 1, 2) ORDER BY created_date ASC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUrl: Int = getColumnIndexOrThrow(_stmt, "url")
        val _columnIndexOfFileName: Int = getColumnIndexOrThrow(_stmt, "file_name")
        val _columnIndexOfSavePath: Int = getColumnIndexOrThrow(_stmt, "save_path")
        val _columnIndexOfTotalBytes: Int = getColumnIndexOrThrow(_stmt, "total_bytes")
        val _columnIndexOfDownloadedBytes: Int = getColumnIndexOrThrow(_stmt, "downloaded_bytes")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfSpeed: Int = getColumnIndexOrThrow(_stmt, "speed")
        val _columnIndexOfThreadsCount: Int = getColumnIndexOrThrow(_stmt, "threads_count")
        val _columnIndexOfCreatedDate: Int = getColumnIndexOrThrow(_stmt, "created_date")
        val _columnIndexOfCompletedDate: Int = getColumnIndexOrThrow(_stmt, "completed_date")
        val _columnIndexOfMimeType: Int = getColumnIndexOrThrow(_stmt, "mime_type")
        val _columnIndexOfUserAgent: Int = getColumnIndexOrThrow(_stmt, "user_agent")
        val _columnIndexOfCookies: Int = getColumnIndexOrThrow(_stmt, "cookies")
        val _columnIndexOfReferer: Int = getColumnIndexOrThrow(_stmt, "referer")
        val _columnIndexOfHeaders: Int = getColumnIndexOrThrow(_stmt, "headers")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfErrorMessage: Int = getColumnIndexOrThrow(_stmt, "error_message")
        val _columnIndexOfIsTorrent: Int = getColumnIndexOrThrow(_stmt, "is_torrent")
        val _columnIndexOfTorrentInfoHash: Int = getColumnIndexOrThrow(_stmt, "torrent_info_hash")
        val _columnIndexOfIsWifiOnly: Int = getColumnIndexOrThrow(_stmt, "is_wifi_only")
        val _result: MutableList<DownloadEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: DownloadEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpUrl: String
          _tmpUrl = _stmt.getText(_columnIndexOfUrl)
          val _tmpFileName: String
          _tmpFileName = _stmt.getText(_columnIndexOfFileName)
          val _tmpSavePath: String
          _tmpSavePath = _stmt.getText(_columnIndexOfSavePath)
          val _tmpTotalBytes: Long
          _tmpTotalBytes = _stmt.getLong(_columnIndexOfTotalBytes)
          val _tmpDownloadedBytes: Long
          _tmpDownloadedBytes = _stmt.getLong(_columnIndexOfDownloadedBytes)
          val _tmpStatus: Int
          _tmpStatus = _stmt.getLong(_columnIndexOfStatus).toInt()
          val _tmpSpeed: Long
          _tmpSpeed = _stmt.getLong(_columnIndexOfSpeed)
          val _tmpThreadsCount: Int
          _tmpThreadsCount = _stmt.getLong(_columnIndexOfThreadsCount).toInt()
          val _tmpCreatedDate: Long
          _tmpCreatedDate = _stmt.getLong(_columnIndexOfCreatedDate)
          val _tmpCompletedDate: Long
          _tmpCompletedDate = _stmt.getLong(_columnIndexOfCompletedDate)
          val _tmpMimeType: String?
          if (_stmt.isNull(_columnIndexOfMimeType)) {
            _tmpMimeType = null
          } else {
            _tmpMimeType = _stmt.getText(_columnIndexOfMimeType)
          }
          val _tmpUserAgent: String?
          if (_stmt.isNull(_columnIndexOfUserAgent)) {
            _tmpUserAgent = null
          } else {
            _tmpUserAgent = _stmt.getText(_columnIndexOfUserAgent)
          }
          val _tmpCookies: String?
          if (_stmt.isNull(_columnIndexOfCookies)) {
            _tmpCookies = null
          } else {
            _tmpCookies = _stmt.getText(_columnIndexOfCookies)
          }
          val _tmpReferer: String?
          if (_stmt.isNull(_columnIndexOfReferer)) {
            _tmpReferer = null
          } else {
            _tmpReferer = _stmt.getText(_columnIndexOfReferer)
          }
          val _tmpHeaders: String?
          if (_stmt.isNull(_columnIndexOfHeaders)) {
            _tmpHeaders = null
          } else {
            _tmpHeaders = _stmt.getText(_columnIndexOfHeaders)
          }
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpErrorMessage: String?
          if (_stmt.isNull(_columnIndexOfErrorMessage)) {
            _tmpErrorMessage = null
          } else {
            _tmpErrorMessage = _stmt.getText(_columnIndexOfErrorMessage)
          }
          val _tmpIsTorrent: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsTorrent).toInt()
          _tmpIsTorrent = _tmp != 0
          val _tmpTorrentInfoHash: String?
          if (_stmt.isNull(_columnIndexOfTorrentInfoHash)) {
            _tmpTorrentInfoHash = null
          } else {
            _tmpTorrentInfoHash = _stmt.getText(_columnIndexOfTorrentInfoHash)
          }
          val _tmpIsWifiOnly: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsWifiOnly).toInt()
          _tmpIsWifiOnly = _tmp_1 != 0
          _item = DownloadEntity(_tmpId,_tmpUrl,_tmpFileName,_tmpSavePath,_tmpTotalBytes,_tmpDownloadedBytes,_tmpStatus,_tmpSpeed,_tmpThreadsCount,_tmpCreatedDate,_tmpCompletedDate,_tmpMimeType,_tmpUserAgent,_tmpCookies,_tmpReferer,_tmpHeaders,_tmpCategory,_tmpErrorMessage,_tmpIsTorrent,_tmpTorrentInfoHash,_tmpIsWifiOnly)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteById(id: Long): Int {
    val _sql: String = "DELETE FROM downloads WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
        getTotalChangedRows(_connection)
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateStatusAndSpeed(
    id: Long,
    status: Int,
    speed: Long,
  ): Int {
    val _sql: String = "UPDATE downloads SET status = ?, speed = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, status.toLong())
        _argIndex = 2
        _stmt.bindLong(_argIndex, speed)
        _argIndex = 3
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
        getTotalChangedRows(_connection)
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateProgressAndSpeed(
    id: Long,
    downloadedBytes: Long,
    speed: Long,
  ): Int {
    val _sql: String = "UPDATE downloads SET downloaded_bytes = ?, speed = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, downloadedBytes)
        _argIndex = 2
        _stmt.bindLong(_argIndex, speed)
        _argIndex = 3
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
        getTotalChangedRows(_connection)
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateAllStatus(oldStatus: Int, newStatus: Int): Int {
    val _sql: String = "UPDATE downloads SET status = ? WHERE status = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, newStatus.toLong())
        _argIndex = 2
        _stmt.bindLong(_argIndex, oldStatus.toLong())
        _stmt.step()
        getTotalChangedRows(_connection)
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
