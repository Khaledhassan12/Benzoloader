package khaled.benzo.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
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
    fun toModel(): SiteProfile = SiteProfile(
        id, domain, username,
        khaled.benzo.core.security.Secrets.open(password), userAgent,
        khaled.benzo.core.security.Secrets.open(customCookies)
    )

    companion object {
        fun fromModel(model: SiteProfile): SiteEntity =
            SiteEntity(
                model.id, model.domain, model.username,
                khaled.benzo.core.security.Secrets.seal(model.password), model.userAgent,
                khaled.benzo.core.security.Secrets.seal(model.customCookies)
            )
    }
}
