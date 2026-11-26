package khaled.benzo.core.files

import khaled.benzo.data.model.DownloadItem
import khaled.benzo.data.model.DownloadStatus

/** Which selected items Open and Send can act on: finished, not a torrent folder, and the file is really there. */
object FileActionPolicy {
    fun usable(items: List<DownloadItem>, exists: (DownloadItem) -> Boolean): List<DownloadItem> =
        items.filter { it.status == DownloadStatus.COMPLETED && !it.isTorrent && exists(it) }

    /** Open acts on exactly one file; with several selected it does nothing and the screen says why. */
    fun openTarget(items: List<DownloadItem>, exists: (DownloadItem) -> Boolean): DownloadItem? = usable(items, exists).singleOrNull().takeIf { items.size == 1 }
}
