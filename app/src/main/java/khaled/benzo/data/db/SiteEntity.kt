package khaled.benzo.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import khaled.benzo.core.security.SecretRead
import khaled.benzo.core.security.Secrets
import khaled.benzo.data.model.SiteProfile

@Entity(tableName = "sites")
data class SiteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "domain") val domain: String,
    @ColumnInfo(name = "username") val username: String? = null,
    @ColumnInfo(name = "password") val password: String? = null,
    @ColumnInfo(name = "user_agent") val userAgent: String? = null,
    @ColumnInfo(name = "custom_cookies") val customCookies: String? = null
) {
    fun toModel(): SiteProfile {
        val pw = Secrets.read(password)
        val ck = Secrets.read(customCookies)
        return SiteProfile(
            id, domain, username,
            (pw as? SecretRead.Ok)?.value, userAgent, (ck as? SecretRead.Ok)?.value,
            (pw as? SecretRead.Unreadable)?.raw, (ck as? SecretRead.Unreadable)?.raw
        )
    }

    companion object {
        fun fromModel(model: SiteProfile): SiteEntity =
            SiteEntity(
                model.id, model.domain, model.username,
                Secrets.sealOrKeep(model.password, model.rawPassword), model.userAgent,
                Secrets.sealOrKeep(model.customCookies, model.rawCookies)
            )
    }
}
