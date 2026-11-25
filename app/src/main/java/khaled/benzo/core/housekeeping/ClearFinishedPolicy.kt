package khaled.benzo.core.housekeeping

import khaled.benzo.data.model.DownloadItem
import khaled.benzo.data.model.DownloadStatus

/**
 * Which list entries "clear finished downloads" removes at app start. Only the list entry goes: downloaded files are never
 * deleted. Torrents stay, because removing the row of a finished torrent would also end its seeding. Nothing is cleared
 * when the setting is off.
 */
object ClearFinishedPolicy {
    fun idsToClear(items: List<DownloadItem>, enabled: Boolean): List<Long> =
        if (!enabled) emptyList()
        else items.filter { it.status == DownloadStatus.COMPLETED && !it.isTorrent }.map { it.id }
}
