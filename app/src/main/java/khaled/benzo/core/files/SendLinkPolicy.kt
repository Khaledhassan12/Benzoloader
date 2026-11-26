package khaled.benzo.core.files

import khaled.benzo.data.model.DownloadItem

/**
 * Link choice for Send. The reference's Send list did not show whether it sends the file or the link (UNCLEAR), so the user picks.
 * A link is only offered after the user sees the full text, and links carrying a query, fragment or sign-in part are flagged.
 */
object SendLinkPolicy {
    fun links(items: List<DownloadItem>): List<String> =
        items.filter { !it.isTorrent }.map { it.url.trim() }
            .filter { it.startsWith("http://", true) || it.startsWith("https://", true) }.distinct()

    /** True when the link may hold a private key or token: a query (?), a fragment (#) or user info (user:pass@). */
    fun mayBePrivate(url: String): Boolean {
        val afterScheme = url.substringAfter("://", url)
        val authority = afterScheme.substringBefore('/').substringBefore('?').substringBefore('#')
        return '?' in url || '#' in url || '@' in authority
    }

    fun anyMayBePrivate(urls: List<String>) = urls.any { mayBePrivate(it) }
}
