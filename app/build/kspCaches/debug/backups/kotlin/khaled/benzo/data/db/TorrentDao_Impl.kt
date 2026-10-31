package khaled.benzo.`data`.db

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
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
public class TorrentDao_Impl(
  __db: RoomDatabase,
) : TorrentDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfTorrentEntity: EntityInsertAdapter<TorrentEntity>

  private val __deleteAdapterOfTorrentEntity: EntityDeleteOrUpdateAdapter<TorrentEntity>

  private val __updateAdapterOfTorrentEntity: EntityDeleteOrUpdateAdapter<TorrentEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfTorrentEntity = object : EntityInsertAdapter<TorrentEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `torrents` (`infoHash`,`name`,`total_size`,`downloaded_size`,`upload_size`,`state`,`save_path`,`torrent_file_path`) VALUES (?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: TorrentEntity) {
        statement.bindText(1, entity.infoHash)
        statement.bindText(2, entity.name)
        statement.bindLong(3, entity.totalSize)
        statement.bindLong(4, entity.downloadedSize)
        statement.bindLong(5, entity.uploadSize)
        statement.bindText(6, entity.state)
        statement.bindText(7, entity.savePath)
        val _tmpTorrentFilePath: String? = entity.torrentFilePath
        if (_tmpTorrentFilePath == null) {
          statement.bindNull(8)
        } else {
          statement.bindText(8, _tmpTorrentFilePath)
        }
      }
    }
    this.__deleteAdapterOfTorrentEntity = object : EntityDeleteOrUpdateAdapter<TorrentEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `torrents` WHERE `infoHash` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: TorrentEntity) {
        statement.bindText(1, entity.infoHash)
      }
    }
    this.__updateAdapterOfTorrentEntity = object : EntityDeleteOrUpdateAdapter<TorrentEntity>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `torrents` SET `infoHash` = ?,`name` = ?,`total_size` = ?,`downloaded_size` = ?,`upload_size` = ?,`state` = ?,`save_path` = ?,`torrent_file_path` = ? WHERE `infoHash` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: TorrentEntity) {
        statement.bindText(1, entity.infoHash)
        statement.bindText(2, entity.name)
        statement.bindLong(3, entity.totalSize)
        statement.bindLong(4, entity.downloadedSize)
        statement.bindLong(5, entity.uploadSize)
        statement.bindText(6, entity.state)
        statement.bindText(7, entity.savePath)
        val _tmpTorrentFilePath: String? = entity.torrentFilePath
        if (_tmpTorrentFilePath == null) {
          statement.bindNull(8)
        } else {
          statement.bindText(8, _tmpTorrentFilePath)
        }
        statement.bindText(9, entity.infoHash)
      }
    }
  }

  public override suspend fun insertTorrent(torrent: TorrentEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfTorrentEntity.insert(_connection, torrent)
  }

  public override suspend fun deleteTorrent(torrent: TorrentEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfTorrentEntity.handle(_connection, torrent)
  }

  public override suspend fun updateTorrent(torrent: TorrentEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfTorrentEntity.handle(_connection, torrent)
  }

  public override suspend fun getTorrentByHash(infoHash: String): TorrentEntity? {
    val _sql: String = "SELECT * FROM torrents WHERE infoHash = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, infoHash)
        val _columnIndexOfInfoHash: Int = getColumnIndexOrThrow(_stmt, "infoHash")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfTotalSize: Int = getColumnIndexOrThrow(_stmt, "total_size")
        val _columnIndexOfDownloadedSize: Int = getColumnIndexOrThrow(_stmt, "downloaded_size")
        val _columnIndexOfUploadSize: Int = getColumnIndexOrThrow(_stmt, "upload_size")
        val _columnIndexOfState: Int = getColumnIndexOrThrow(_stmt, "state")
        val _columnIndexOfSavePath: Int = getColumnIndexOrThrow(_stmt, "save_path")
        val _columnIndexOfTorrentFilePath: Int = getColumnIndexOrThrow(_stmt, "torrent_file_path")
        val _result: TorrentEntity?
        if (_stmt.step()) {
          val _tmpInfoHash: String
          _tmpInfoHash = _stmt.getText(_columnIndexOfInfoHash)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpTotalSize: Long
          _tmpTotalSize = _stmt.getLong(_columnIndexOfTotalSize)
          val _tmpDownloadedSize: Long
          _tmpDownloadedSize = _stmt.getLong(_columnIndexOfDownloadedSize)
          val _tmpUploadSize: Long
          _tmpUploadSize = _stmt.getLong(_columnIndexOfUploadSize)
          val _tmpState: String
          _tmpState = _stmt.getText(_columnIndexOfState)
          val _tmpSavePath: String
          _tmpSavePath = _stmt.getText(_columnIndexOfSavePath)
          val _tmpTorrentFilePath: String?
          if (_stmt.isNull(_columnIndexOfTorrentFilePath)) {
            _tmpTorrentFilePath = null
          } else {
            _tmpTorrentFilePath = _stmt.getText(_columnIndexOfTorrentFilePath)
          }
          _result = TorrentEntity(_tmpInfoHash,_tmpName,_tmpTotalSize,_tmpDownloadedSize,_tmpUploadSize,_tmpState,_tmpSavePath,_tmpTorrentFilePath)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getAllTorrentsFlow(): Flow<List<TorrentEntity>> {
    val _sql: String = "SELECT * FROM torrents"
    return createFlow(__db, false, arrayOf("torrents")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfInfoHash: Int = getColumnIndexOrThrow(_stmt, "infoHash")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfTotalSize: Int = getColumnIndexOrThrow(_stmt, "total_size")
        val _columnIndexOfDownloadedSize: Int = getColumnIndexOrThrow(_stmt, "downloaded_size")
        val _columnIndexOfUploadSize: Int = getColumnIndexOrThrow(_stmt, "upload_size")
        val _columnIndexOfState: Int = getColumnIndexOrThrow(_stmt, "state")
        val _columnIndexOfSavePath: Int = getColumnIndexOrThrow(_stmt, "save_path")
        val _columnIndexOfTorrentFilePath: Int = getColumnIndexOrThrow(_stmt, "torrent_file_path")
        val _result: MutableList<TorrentEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TorrentEntity
          val _tmpInfoHash: String
          _tmpInfoHash = _stmt.getText(_columnIndexOfInfoHash)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpTotalSize: Long
          _tmpTotalSize = _stmt.getLong(_columnIndexOfTotalSize)
          val _tmpDownloadedSize: Long
          _tmpDownloadedSize = _stmt.getLong(_columnIndexOfDownloadedSize)
          val _tmpUploadSize: Long
          _tmpUploadSize = _stmt.getLong(_columnIndexOfUploadSize)
          val _tmpState: String
          _tmpState = _stmt.getText(_columnIndexOfState)
          val _tmpSavePath: String
          _tmpSavePath = _stmt.getText(_columnIndexOfSavePath)
          val _tmpTorrentFilePath: String?
          if (_stmt.isNull(_columnIndexOfTorrentFilePath)) {
            _tmpTorrentFilePath = null
          } else {
            _tmpTorrentFilePath = _stmt.getText(_columnIndexOfTorrentFilePath)
          }
          _item = TorrentEntity(_tmpInfoHash,_tmpName,_tmpTotalSize,_tmpDownloadedSize,_tmpUploadSize,_tmpState,_tmpSavePath,_tmpTorrentFilePath)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
