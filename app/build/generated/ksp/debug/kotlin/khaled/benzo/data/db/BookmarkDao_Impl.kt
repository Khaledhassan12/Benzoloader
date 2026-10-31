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
public class BookmarkDao_Impl(
  __db: RoomDatabase,
) : BookmarkDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfBookmarkEntity: EntityInsertAdapter<BookmarkEntity>

  private val __deleteAdapterOfBookmarkEntity: EntityDeleteOrUpdateAdapter<BookmarkEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfBookmarkEntity = object : EntityInsertAdapter<BookmarkEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `bookmarks` (`id`,`title`,`url`,`created_at`) VALUES (nullif(?, 0),?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: BookmarkEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.title)
        statement.bindText(3, entity.url)
        statement.bindLong(4, entity.createdAt)
      }
    }
    this.__deleteAdapterOfBookmarkEntity = object : EntityDeleteOrUpdateAdapter<BookmarkEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `bookmarks` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: BookmarkEntity) {
        statement.bindLong(1, entity.id)
      }
    }
  }

  public override suspend fun insertBookmark(bookmark: BookmarkEntity): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfBookmarkEntity.insertAndReturnId(_connection, bookmark)
    _result
  }

  public override suspend fun deleteBookmark(bookmark: BookmarkEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfBookmarkEntity.handle(_connection, bookmark)
  }

  public override fun getAllBookmarksFlow(): Flow<List<BookmarkEntity>> {
    val _sql: String = "SELECT * FROM bookmarks ORDER BY created_at DESC"
    return createFlow(__db, false, arrayOf("bookmarks")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfUrl: Int = getColumnIndexOrThrow(_stmt, "url")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "created_at")
        val _result: MutableList<BookmarkEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: BookmarkEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpUrl: String
          _tmpUrl = _stmt.getText(_columnIndexOfUrl)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _item = BookmarkEntity(_tmpId,_tmpTitle,_tmpUrl,_tmpCreatedAt)
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
