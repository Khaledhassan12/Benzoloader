package khaled.benzo.core.data

import android.content.Context
import android.os.Environment
import khaled.benzo.data.db.BenzoDatabase
import khaled.benzo.data.model.DownloadItem
import khaled.benzo.data.model.DownloadStatus
import khaled.benzo.data.repository.DownloadRepository
import khaled.benzo.data.repository.SettingsRepository
import java.io.File

data class ImportResult(val added: Int, val skipped: Int)

/** Turns the text of an imported list into paused downloads. Parsing itself lives in [ListTransfer]. */
class ListImporter(private val context: Context, private val repository: DownloadRepository) {

    suspend fun importText(text: String): ImportResult {
        val settings = SettingsRepository(context)
        val root = settings.defaultDownloadPath.ifEmpty {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
        }
        val existing = BenzoDatabase.getInstance(context).downloadDao().getAllOnce()
        val knownUrls = existing.map { it.url }.toSet()
        val taken = existing.map { it.savePath to it.fileName }.toSet()
        val entries = ListTransfer.parse(text)
        val planner = ImportPlanner(root, settings.catalogEnabled, settings.catalogFolderNames) { folder, name -> File(folder, name).exists() }
        val planned = planner.plan(entries, knownUrls, taken)
        for (p in planned) {
            repository.insertDownload(
                DownloadItem(
                    url = p.url, fileName = p.fileName, savePath = p.folder,
                    status = DownloadStatus.PAUSED, category = p.category,
                    threadsCount = settings.defaultThreadsCount, isWifiOnly = settings.isWifiOnly
                )
            )
        }
        val added = planned.size
        return ImportResult(added, entries.size - added)
    }
}
