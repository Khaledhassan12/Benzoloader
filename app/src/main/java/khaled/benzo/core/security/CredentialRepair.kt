package khaled.benzo.core.security

import khaled.benzo.data.model.DownloadItem
import khaled.benzo.data.model.DownloadStatus

/** Re-entry of cookies or headers that can no longer be decrypted. Progress, chunks and files are never touched. */
object CredentialRepair {
    /**
     * [cookies] and [headers]: a non-blank value replaces the unreadable one. A blank value keeps the stored text as it is.
     * [discardUnreadable]: the user chose to continue without the unreadable values (they are dropped for good).
     * The download becomes Paused with no error, so the user resumes it. Returns the item unchanged when nothing applies.
     */
    fun apply(item: DownloadItem, cookies: String?, headers: String?, discardUnreadable: Boolean): DownloadItem {
        if (!item.needsReentry) return item
        var out = item
        if (item.rawCookies != null) {
            if (!cookies.isNullOrBlank()) out = out.copy(cookies = cookies.trim(), rawCookies = null)
            else if (discardUnreadable) out = out.copy(cookies = null, rawCookies = null)
        }
        if (item.rawHeaders != null) {
            if (!headers.isNullOrBlank()) out = out.copy(headersJson = headers.trim(), rawHeaders = null)
            else if (discardUnreadable) out = out.copy(headersJson = null, rawHeaders = null)
        }
        return if (out.needsReentry) out else out.copy(status = DownloadStatus.PAUSED, errorMessage = null, speed = 0L)
    }
}
