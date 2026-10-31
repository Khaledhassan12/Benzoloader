package khaled.benzo.data.model

data class SiteProfile(
    val id: Long = 0,
    val domain: String,
    val username: String? = null,
    val password: String? = null,
    val userAgent: String? = null,
    val customCookies: String? = null
)
