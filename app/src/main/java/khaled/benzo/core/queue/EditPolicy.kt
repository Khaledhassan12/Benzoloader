package khaled.benzo.core.queue

import khaled.benzo.data.model.DownloadItem
import khaled.benzo.data.model.DownloadStatus

/**
 * What "Change" may do to an existing item. Link, name and folder identify the partial file on disk, so they can only
 * change while nothing has been downloaded and nothing is running. Speed limit, checksum, category, connections and
 * Wi-Fi rule never touch the file, so they are always allowed.
 */
object EditPolicy {
    enum class Verdict { OK, LOCKED_BY_PROGRESS }

    fun check(old: DownloadItem, newUrl: String, newName: String, newPath: String): Verdict {
        val identityChanged = newUrl != old.url || newName != old.fileName || newPath != old.savePath
        val hasData = old.downloadedBytes > 0 || old.status == DownloadStatus.RUNNING
        return if (identityChanged && hasData) Verdict.LOCKED_BY_PROGRESS else Verdict.OK
    }

    /** Running items keep the settings they started with, so the user is told the change applies on the next start. */
    fun appliesNextStart(old: DownloadItem) = old.status == DownloadStatus.RUNNING
}
