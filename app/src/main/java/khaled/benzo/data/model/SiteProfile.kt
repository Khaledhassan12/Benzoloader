package khaled.benzo.data.model

data class SiteProfile(
    val id: Long = 0,
    val domain: String,
    val username: String? = null,
    val password: String? = null,
    val userAgent: String? = null,
    val customCookies: String? = null,
    /** Stored values that could not be decrypted, kept as stored. */
    val rawPassword: String? = null,
    val rawCookies: String? = null
) {
    val needsReentry: Boolean
        get() = rawPassword != null || rawCookies != null
}
