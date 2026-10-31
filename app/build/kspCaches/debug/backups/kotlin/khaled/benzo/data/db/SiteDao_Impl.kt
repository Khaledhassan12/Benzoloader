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
public class SiteDao_Impl(
  __db: RoomDatabase,
) : SiteDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfSiteEntity: EntityInsertAdapter<SiteEntity>

  private val __deleteAdapterOfSiteEntity: EntityDeleteOrUpdateAdapter<SiteEntity>

  private val __updateAdapterOfSiteEntity: EntityDeleteOrUpdateAdapter<SiteEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfSiteEntity = object : EntityInsertAdapter<SiteEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `sites` (`id`,`domain`,`username`,`password`,`user_agent`,`custom_cookies`) VALUES (nullif(?, 0),?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: SiteEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.domain)
        val _tmpUsername: String? = entity.username
        if (_tmpUsername == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpUsername)
        }
        val _tmpPassword: String? = entity.password
        if (_tmpPassword == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpPassword)
        }
        val _tmpUserAgent: String? = entity.userAgent
        if (_tmpUserAgent == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpUserAgent)
        }
        val _tmpCustomCookies: String? = entity.customCookies
        if (_tmpCustomCookies == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpCustomCookies)
        }
      }
    }
    this.__deleteAdapterOfSiteEntity = object : EntityDeleteOrUpdateAdapter<SiteEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `sites` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: SiteEntity) {
        statement.bindLong(1, entity.id)
      }
    }
    this.__updateAdapterOfSiteEntity = object : EntityDeleteOrUpdateAdapter<SiteEntity>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `sites` SET `id` = ?,`domain` = ?,`username` = ?,`password` = ?,`user_agent` = ?,`custom_cookies` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: SiteEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.domain)
        val _tmpUsername: String? = entity.username
        if (_tmpUsername == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpUsername)
        }
        val _tmpPassword: String? = entity.password
        if (_tmpPassword == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpPassword)
        }
        val _tmpUserAgent: String? = entity.userAgent
        if (_tmpUserAgent == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpUserAgent)
        }
        val _tmpCustomCookies: String? = entity.customCookies
        if (_tmpCustomCookies == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpCustomCookies)
        }
        statement.bindLong(7, entity.id)
      }
    }
  }

  public override suspend fun insertSite(site: SiteEntity): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfSiteEntity.insertAndReturnId(_connection, site)
    _result
  }

  public override suspend fun deleteSite(site: SiteEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfSiteEntity.handle(_connection, site)
  }

  public override suspend fun updateSite(site: SiteEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfSiteEntity.handle(_connection, site)
  }

  public override suspend fun getSiteByDomain(domain: String): SiteEntity? {
    val _sql: String = "SELECT * FROM sites WHERE domain = ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, domain)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfDomain: Int = getColumnIndexOrThrow(_stmt, "domain")
        val _columnIndexOfUsername: Int = getColumnIndexOrThrow(_stmt, "username")
        val _columnIndexOfPassword: Int = getColumnIndexOrThrow(_stmt, "password")
        val _columnIndexOfUserAgent: Int = getColumnIndexOrThrow(_stmt, "user_agent")
        val _columnIndexOfCustomCookies: Int = getColumnIndexOrThrow(_stmt, "custom_cookies")
        val _result: SiteEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpDomain: String
          _tmpDomain = _stmt.getText(_columnIndexOfDomain)
          val _tmpUsername: String?
          if (_stmt.isNull(_columnIndexOfUsername)) {
            _tmpUsername = null
          } else {
            _tmpUsername = _stmt.getText(_columnIndexOfUsername)
          }
          val _tmpPassword: String?
          if (_stmt.isNull(_columnIndexOfPassword)) {
            _tmpPassword = null
          } else {
            _tmpPassword = _stmt.getText(_columnIndexOfPassword)
          }
          val _tmpUserAgent: String?
          if (_stmt.isNull(_columnIndexOfUserAgent)) {
            _tmpUserAgent = null
          } else {
            _tmpUserAgent = _stmt.getText(_columnIndexOfUserAgent)
          }
          val _tmpCustomCookies: String?
          if (_stmt.isNull(_columnIndexOfCustomCookies)) {
            _tmpCustomCookies = null
          } else {
            _tmpCustomCookies = _stmt.getText(_columnIndexOfCustomCookies)
          }
          _result = SiteEntity(_tmpId,_tmpDomain,_tmpUsername,_tmpPassword,_tmpUserAgent,_tmpCustomCookies)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAllSites(): List<SiteEntity> {
    val _sql: String = "SELECT * FROM sites ORDER BY domain ASC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfDomain: Int = getColumnIndexOrThrow(_stmt, "domain")
        val _columnIndexOfUsername: Int = getColumnIndexOrThrow(_stmt, "username")
        val _columnIndexOfPassword: Int = getColumnIndexOrThrow(_stmt, "password")
        val _columnIndexOfUserAgent: Int = getColumnIndexOrThrow(_stmt, "user_agent")
        val _columnIndexOfCustomCookies: Int = getColumnIndexOrThrow(_stmt, "custom_cookies")
        val _result: MutableList<SiteEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: SiteEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpDomain: String
          _tmpDomain = _stmt.getText(_columnIndexOfDomain)
          val _tmpUsername: String?
          if (_stmt.isNull(_columnIndexOfUsername)) {
            _tmpUsername = null
          } else {
            _tmpUsername = _stmt.getText(_columnIndexOfUsername)
          }
          val _tmpPassword: String?
          if (_stmt.isNull(_columnIndexOfPassword)) {
            _tmpPassword = null
          } else {
            _tmpPassword = _stmt.getText(_columnIndexOfPassword)
          }
          val _tmpUserAgent: String?
          if (_stmt.isNull(_columnIndexOfUserAgent)) {
            _tmpUserAgent = null
          } else {
            _tmpUserAgent = _stmt.getText(_columnIndexOfUserAgent)
          }
          val _tmpCustomCookies: String?
          if (_stmt.isNull(_columnIndexOfCustomCookies)) {
            _tmpCustomCookies = null
          } else {
            _tmpCustomCookies = _stmt.getText(_columnIndexOfCustomCookies)
          }
          _item = SiteEntity(_tmpId,_tmpDomain,_tmpUsername,_tmpPassword,_tmpUserAgent,_tmpCustomCookies)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getAllSitesFlow(): Flow<List<SiteEntity>> {
    val _sql: String = "SELECT * FROM sites ORDER BY domain ASC"
    return createFlow(__db, false, arrayOf("sites")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfDomain: Int = getColumnIndexOrThrow(_stmt, "domain")
        val _columnIndexOfUsername: Int = getColumnIndexOrThrow(_stmt, "username")
        val _columnIndexOfPassword: Int = getColumnIndexOrThrow(_stmt, "password")
        val _columnIndexOfUserAgent: Int = getColumnIndexOrThrow(_stmt, "user_agent")
        val _columnIndexOfCustomCookies: Int = getColumnIndexOrThrow(_stmt, "custom_cookies")
        val _result: MutableList<SiteEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: SiteEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpDomain: String
          _tmpDomain = _stmt.getText(_columnIndexOfDomain)
          val _tmpUsername: String?
          if (_stmt.isNull(_columnIndexOfUsername)) {
            _tmpUsername = null
          } else {
            _tmpUsername = _stmt.getText(_columnIndexOfUsername)
          }
          val _tmpPassword: String?
          if (_stmt.isNull(_columnIndexOfPassword)) {
            _tmpPassword = null
          } else {
            _tmpPassword = _stmt.getText(_columnIndexOfPassword)
          }
          val _tmpUserAgent: String?
          if (_stmt.isNull(_columnIndexOfUserAgent)) {
            _tmpUserAgent = null
          } else {
            _tmpUserAgent = _stmt.getText(_columnIndexOfUserAgent)
          }
          val _tmpCustomCookies: String?
          if (_stmt.isNull(_columnIndexOfCustomCookies)) {
            _tmpCustomCookies = null
          } else {
            _tmpCustomCookies = _stmt.getText(_columnIndexOfCustomCookies)
          }
          _item = SiteEntity(_tmpId,_tmpDomain,_tmpUsername,_tmpPassword,_tmpUserAgent,_tmpCustomCookies)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteByDomain(domain: String): Int {
    val _sql: String = "DELETE FROM sites WHERE domain = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, domain)
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
