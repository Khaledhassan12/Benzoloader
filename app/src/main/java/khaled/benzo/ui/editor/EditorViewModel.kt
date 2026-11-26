package khaled.benzo.ui.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import khaled.benzo.data.model.DownloadItem
import khaled.benzo.data.model.DownloadStatus
import khaled.benzo.data.model.FileCategory
import khaled.benzo.data.repository.DownloadRepository
import khaled.benzo.data.repository.SettingsRepository
import kotlinx.coroutines.launch

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DownloadRepository(application)
    val settingsRepository = SettingsRepository(application)

    enum class ChangeResult { SAVED, LOCKED, COLLISION }

    /**
     * Saves edits to an existing item with targeted updates only: the stored row is never rewritten from the screen's copy,
     * so progress and status written by a running download are not overwritten, and cookies, user agent, referer and
     * headers are not touched at all. Everything is re-read from the database here, not taken from the screen's earlier copy.
     */
    fun changeDownload(
        id: Long, url: String, fileName: String, savePath: String, parts: Int, isWifiOnly: Boolean,
        category: FileCategory?, speedLimitKBps: Int, expectedChecksum: String?, onDone: (ChangeResult, Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val fresh = repository.getDownload(id) ?: return@launch onDone(ChangeResult.LOCKED, false)
            val (oldExists, newExists, others) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                Triple(java.io.File(fresh.savePath, fresh.fileName).exists(), java.io.File(savePath, fileName).exists(), repository.getAllOnce())
            }
            when (khaled.benzo.core.queue.EditPolicy.check(fresh, url, fileName, savePath, oldExists, others, newExists)) {
                khaled.benzo.core.queue.EditPolicy.Verdict.LOCKED_BY_PROGRESS -> return@launch onDone(ChangeResult.LOCKED, false)
                khaled.benzo.core.queue.EditPolicy.Verdict.COLLISION -> return@launch onDone(ChangeResult.COLLISION, false)
                else -> {}
            }
            val identityChanged = url != fresh.url || fileName != fresh.fileName || savePath != fresh.savePath
            if (identityChanged && repository.updateIdentityGuarded(id, url, fileName, savePath) == 0) return@launch onDone(ChangeResult.LOCKED, false)
            repository.updateSafeFields(id, parts, isWifiOnly, category ?: fresh.category, speedLimitKBps, expectedChecksum)
            onDone(ChangeResult.SAVED, khaled.benzo.core.queue.EditPolicy.appliesNextStart(fresh))
        }
    }

    suspend fun load(id: Long): DownloadItem? = repository.getDownload(id)

    fun addDownload(
        url: String,
        fileName: String,
        savePath: String,
        parts: Int,
        isWifiOnly: Boolean,
        userAgent: String?,
        referer: String?,
        cookies: String?,
        startNow: Boolean,
        pickedCategory: FileCategory? = null,
        speedLimitKBps: Int = 0,
        expectedChecksum: String? = null,
        onError: () -> Unit = {},
        onComplete: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val category = pickedCategory ?: FileCategory.fromExtension(fileName)
            val root = settingsRepository.defaultDownloadPath.ifEmpty {
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS).absolutePath
            }
            val finalPath = khaled.benzo.core.catalog.CatalogPolicy.targetFolder(
                savePath, root, category, settingsRepository.catalogEnabled, settingsRepository.catalogFolderNames)
            val queuedNames = khaled.benzo.data.db.BenzoDatabase.getInstance(getApplication())
                .downloadDao().getPendingAndActiveDownloads()
                .filter { it.savePath == finalPath }.map { it.fileName }.toSet()
            val uniqueFileName = khaled.benzo.core.files.FileNaming.uniqueName(fileName) { candidate ->
                candidate in queuedNames || java.io.File(finalPath, candidate).exists()
            }
            val status = khaled.benzo.core.queue.QuickAddPolicy.initialStatus(startNow)

            val item = DownloadItem(
                id = 0L,
                url = url,
                fileName = uniqueFileName,
                savePath = finalPath,
                totalBytes = -1L,
                downloadedBytes = 0L,
                status = status,
                category = category,
                threadsCount = parts,
                speed = 0L,
                createdDate = System.currentTimeMillis(),
                isWifiOnly = isWifiOnly,
                speedLimitKBps = speedLimitKBps,
                expectedChecksum = expectedChecksum,
                userAgent = userAgent,
                referer = referer,
                cookies = cookies
            )

            val newId = runCatching { repository.insertDownload(item) }.getOrNull()
            // Saving can fail when the secure key store is unavailable. Report it, never store the cookies unencrypted.
            if (newId == null) onError() else onComplete(newId)
        }
    }
}
