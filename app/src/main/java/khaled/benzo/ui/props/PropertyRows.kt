package khaled.benzo.ui.props

import khaled.benzo.data.model.DownloadItem
import khaled.benzo.data.model.DownloadStatus

/** One line of the Properties screen. [key] is stable (used for the hide list); labels are mapped to strings by the screen. */
data class PropRow(val key: String, val value: String)

/** Builds only rows the app really has data for. Nothing is invented for fields the engine does not track (pieces, peers). */
object PropertyRows {
    fun build(d: DownloadItem, size: (Long) -> String, time: (Long) -> String): List<PropRow> = buildList {
        add(PropRow("name", d.fileName))
        add(PropRow("category", d.category.name))
        add(PropRow("path", d.savePath))
        add(PropRow("url", d.url))
        add(PropRow("size", if (d.totalBytes > 0) size(d.totalBytes) else "?"))
        add(PropRow("downloaded", size(d.downloadedBytes)))
        if (d.status == DownloadStatus.RUNNING && d.speed > 0) add(PropRow("speed", size(d.speed) + "/s"))
        d.errorMessage?.takeIf { it.isNotBlank() }?.let { add(PropRow("message", it)) }
        if (!d.isTorrent) add(PropRow("threads", d.parts.toString()))
        if (d.speedLimitKBps > 0) add(PropRow("limit", "${d.speedLimitKBps} KB/s"))
        d.expectedChecksum?.takeIf { it.isNotBlank() }?.let { add(PropRow("checksum", it)) }
        add(PropRow("created", time(d.createdDate)))
        if (d.completedDate > 0) add(PropRow("finished", time(d.completedDate)))
        d.torrentInfoHash?.takeIf { d.isTorrent }?.let { add(PropRow("hash", it)) }
        d.userAgent?.takeIf { it.isNotBlank() }?.let { add(PropRow("agent", it)) }
    }

    fun visible(rows: List<PropRow>, hidden: Set<String>) = rows.filter { it.key !in hidden }
}
