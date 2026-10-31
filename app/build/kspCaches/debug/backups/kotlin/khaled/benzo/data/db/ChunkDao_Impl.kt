package khaled.benzo.`data`.db

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.getTotalChangedRows
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

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class ChunkDao_Impl(
  __db: RoomDatabase,
) : ChunkDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfChunkEntity: EntityInsertAdapter<ChunkEntity>

  private val __updateAdapterOfChunkEntity: EntityDeleteOrUpdateAdapter<ChunkEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfChunkEntity = object : EntityInsertAdapter<ChunkEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `chunks` (`chunkId`,`download_id`,`part_index`,`start_byte`,`current_byte`,`end_byte`,`status`) VALUES (nullif(?, 0),?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ChunkEntity) {
        statement.bindLong(1, entity.chunkId)
        statement.bindLong(2, entity.downloadId)
        statement.bindLong(3, entity.partIndex.toLong())
        statement.bindLong(4, entity.startByte)
        statement.bindLong(5, entity.currentByte)
        statement.bindLong(6, entity.endByte)
        statement.bindLong(7, entity.status.toLong())
      }
    }
    this.__updateAdapterOfChunkEntity = object : EntityDeleteOrUpdateAdapter<ChunkEntity>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `chunks` SET `chunkId` = ?,`download_id` = ?,`part_index` = ?,`start_byte` = ?,`current_byte` = ?,`end_byte` = ?,`status` = ? WHERE `chunkId` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: ChunkEntity) {
        statement.bindLong(1, entity.chunkId)
        statement.bindLong(2, entity.downloadId)
        statement.bindLong(3, entity.partIndex.toLong())
        statement.bindLong(4, entity.startByte)
        statement.bindLong(5, entity.currentByte)
        statement.bindLong(6, entity.endByte)
        statement.bindLong(7, entity.status.toLong())
        statement.bindLong(8, entity.chunkId)
      }
    }
  }

  public override suspend fun insertChunks(chunks: List<ChunkEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfChunkEntity.insert(_connection, chunks)
  }

  public override suspend fun updateChunk(chunk: ChunkEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfChunkEntity.handle(_connection, chunk)
  }

  public override suspend fun getChunksForDownload(downloadId: Long): List<ChunkEntity> {
    val _sql: String = "SELECT * FROM chunks WHERE download_id = ? ORDER BY part_index ASC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, downloadId)
        val _columnIndexOfChunkId: Int = getColumnIndexOrThrow(_stmt, "chunkId")
        val _columnIndexOfDownloadId: Int = getColumnIndexOrThrow(_stmt, "download_id")
        val _columnIndexOfPartIndex: Int = getColumnIndexOrThrow(_stmt, "part_index")
        val _columnIndexOfStartByte: Int = getColumnIndexOrThrow(_stmt, "start_byte")
        val _columnIndexOfCurrentByte: Int = getColumnIndexOrThrow(_stmt, "current_byte")
        val _columnIndexOfEndByte: Int = getColumnIndexOrThrow(_stmt, "end_byte")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _result: MutableList<ChunkEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ChunkEntity
          val _tmpChunkId: Long
          _tmpChunkId = _stmt.getLong(_columnIndexOfChunkId)
          val _tmpDownloadId: Long
          _tmpDownloadId = _stmt.getLong(_columnIndexOfDownloadId)
          val _tmpPartIndex: Int
          _tmpPartIndex = _stmt.getLong(_columnIndexOfPartIndex).toInt()
          val _tmpStartByte: Long
          _tmpStartByte = _stmt.getLong(_columnIndexOfStartByte)
          val _tmpCurrentByte: Long
          _tmpCurrentByte = _stmt.getLong(_columnIndexOfCurrentByte)
          val _tmpEndByte: Long
          _tmpEndByte = _stmt.getLong(_columnIndexOfEndByte)
          val _tmpStatus: Int
          _tmpStatus = _stmt.getLong(_columnIndexOfStatus).toInt()
          _item = ChunkEntity(_tmpChunkId,_tmpDownloadId,_tmpPartIndex,_tmpStartByte,_tmpCurrentByte,_tmpEndByte,_tmpStatus)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateChunkProgress(
    chunkId: Long,
    currentByte: Long,
    status: Int,
  ): Int {
    val _sql: String = "UPDATE chunks SET current_byte = ?, status = ? WHERE chunkId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, currentByte)
        _argIndex = 2
        _stmt.bindLong(_argIndex, status.toLong())
        _argIndex = 3
        _stmt.bindLong(_argIndex, chunkId)
        _stmt.step()
        getTotalChangedRows(_connection)
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteChunksForDownload(downloadId: Long): Int {
    val _sql: String = "DELETE FROM chunks WHERE download_id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, downloadId)
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
