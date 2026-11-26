package khaled.benzo.core.queue

import khaled.benzo.data.model.DownloadItem
import khaled.benzo.data.model.DownloadStatus

/**
 * What "Change" may do to an existing item. Link, name and folder identify the file on disk, so they can only change
 * while the item has no data: nothing downloaded, not connecting or running, not completed, and no file at the old
 * location. A new name and folder must not match another item or an existing file. Speed limit, checksum, category,
 * connections and Wi-Fi rule never touch a file, so they are always allowed.
 * The database update repeats the progress and status guard in SQL, so a transfer that starts between the check and
 * the save cannot be edited under (see DownloadDao.updateIdentityGuarded).
 */
object EditPolicy {
    enum class Verdict { OK, LOCKED_BY_PROGRESS, COLLISION }

    private fun inFlight(s: DownloadStatus) = s == DownloadStatus.RUNNING || s == DownloadStatus.CONNECTING

    fun check(
        old: DownloadItem, newUrl: String, newName: String, newPath: String,
        oldFileExists: Boolean = false, others: List<DownloadItem> = emptyList(), newFileExists: Boolean = false
    ): Verdict {
        val moved = newName != old.fileName || newPath != old.savePath
        val identityChanged = moved || newUrl != old.url
        if (!identityChanged) return Verdict.OK
        val hasData = old.downloadedBytes > 0 || inFlight(old.status) || old.status == DownloadStatus.COMPLETED || oldFileExists
        if (hasData) return Verdict.LOCKED_BY_PROGRESS
        if (moved && (newFileExists || others.any { it.id != old.id && it.fileName == newName && it.savePath == newPath })) return Verdict.COLLISION
        return Verdict.OK
    }

    /** Running items keep the settings they started with, so the user is told the change applies on the next start. */
    fun appliesNextStart(old: DownloadItem) = inFlight(old.status)
}
