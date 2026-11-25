package khaled.benzo.core.data

import android.content.Context
import android.os.Environment
import khaled.benzo.core.catalog.CatalogPolicy
import khaled.benzo.core.files.FileNaming
import khaled.benzo.data.db.BenzoDatabase
import khaled.benzo.data.model.DownloadItem
import khaled.benzo.data.model.DownloadStatus
import khaled.benzo.data.model.FileCategory
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
        val knownUrls = existing.map { it.url }.toMutableSet()
        val taken = existing.map { it.savePath to it.fileName }.toMutableSet()
        val entries = ListTransfer.parse(text)
        var added = 0
        for (e in entries) {
            if (e.url in knownUrls) continue
            val raw = ListTransfer.nameFor(e)
            val category = FileCategory.fromExtension(raw)
            val folder = CatalogPolicy.targetFolder(root, root, category, settings.catalogEnabled, settings.catalogFolderNames)
            val name = FileNaming.uniqueName(raw) { c -> (folder to c) in taken || File(folder, c).exists() }
            taken.add(folder to name)
            knownUrls.add(e.url)
            repository.insertDownload(
                DownloadItem(
                    url = e.url, fileName = name, savePath = folder,
                    status = DownloadStatus.PAUSED, category = category,
                    threadsCount = settings.defaultThreadsCount, isWifiOnly = settings.isWifiOnly
                )
            )
            added++
        }
        return ImportResult(added, entries.size - added)
    }
}
