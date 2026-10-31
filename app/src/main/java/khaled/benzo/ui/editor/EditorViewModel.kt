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
        onComplete: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val category = FileCategory.fromExtension(fileName)
            val status = if (startNow) DownloadStatus.QUEUED else DownloadStatus.PAUSED

            val item = DownloadItem(
                id = 0L,
                url = url,
                fileName = fileName,
                savePath = savePath,
                totalBytes = -1L,
                downloadedBytes = 0L,
                status = status,
                category = category,
                threadsCount = parts,
                speed = 0L,
                createdDate = System.currentTimeMillis(),
                isWifiOnly = isWifiOnly,
                userAgent = userAgent,
                referer = referer,
                cookies = cookies
            )

            val newId = repository.insertDownload(item)
            onComplete(newId)
        }
    }
}
