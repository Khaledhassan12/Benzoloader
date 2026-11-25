package khaled.benzo.core.download

import khaled.benzo.data.model.DownloadItem

/** Builds the extra request headers from the editor fields of a download. Values are never logged. */
object RequestHeaders {
    /** The download's own user-agent wins; [fallbackUserAgent] (the Settings value) is used only when it has none. */
    fun from(item: DownloadItem, fallbackUserAgent: String = ""): Map<String, String> = buildMap {
        (item.userAgent?.takeIf { it.isNotBlank() } ?: fallbackUserAgent.takeIf { it.isNotBlank() })?.let { put("User-Agent", it) }
        item.referer?.takeIf { it.isNotBlank() }?.let { put("Referer", it) }
        item.cookies?.takeIf { it.isNotBlank() }?.let { put("Cookie", it) }
    }
}
