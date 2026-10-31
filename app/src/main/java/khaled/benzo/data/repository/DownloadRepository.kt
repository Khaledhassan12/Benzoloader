package khaled.benzo.data.repository

import android.content.Context
import khaled.benzo.data.db.BenzoDatabase
import khaled.benzo.data.db.ChunkEntity
import khaled.benzo.data.db.DownloadEntity
import khaled.benzo.data.model.DownloadChunk
import khaled.benzo.data.model.DownloadItem
import khaled.benzo.data.model.DownloadStatus
import khaled.benzo.data.model.FileCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DownloadRepository(context: Context) {

    private val db = BenzoDatabase.getInstance(context)
    private val downloadDao = db.downloadDao()
    private val chunkDao = db.chunkDao()

    fun getAllDownloads(category: FileCategory?): Flow<List<DownloadItem>> {
        val flow = when (category) {
            null, FileCategory.ALL -> downloadDao.getAllDownloadsFlow()
            FileCategory.DOWNLOADING -> downloadDao.getDownloadsByStatusFlow(DownloadStatus.RUNNING.code)
            FileCategory.COMPLETED -> downloadDao.getDownloadsByStatusFlow(DownloadStatus.COMPLETED.code)
            FileCategory.QUEUED -> downloadDao.getDownloadsByStatusFlow(DownloadStatus.QUEUED.code)
            else -> downloadDao.getDownloadsByCategoryFlow(category.key)
        }
        return flow.map { list -> list.map { it.toModel() } }
    }

    suspend fun getDownload(id: Long): DownloadItem? {
        return downloadDao.getDownloadById(id)?.toModel()
    }

    suspend fun insertDownload(item: DownloadItem): Long {
        return downloadDao.insertDownload(DownloadEntity.fromModel(item))
    }

    suspend fun updateDownload(item: DownloadItem) {
        downloadDao.updateDownload(DownloadEntity.fromModel(item))
    }

    suspend fun deleteDownload(id: Long) {
        chunkDao.deleteChunksForDownload(id)
        downloadDao.deleteById(id)
    }

    suspend fun updateStatusAndSpeed(id: Long, status: DownloadStatus, speed: Long) {
        downloadDao.updateStatusAndSpeed(id, status.code, speed)
    }

    suspend fun updateProgress(id: Long, downloadedBytes: Long, speed: Long) {
        downloadDao.updateProgressAndSpeed(id, downloadedBytes, speed)
    }

    suspend fun getChunks(downloadId: Long): List<DownloadChunk> {
        return chunkDao.getChunksForDownload(downloadId).map { it.toModel() }
    }

    suspend fun saveChunks(chunks: List<DownloadChunk>) {
        chunkDao.insertChunks(chunks.map { ChunkEntity.fromModel(it) })
    }

    suspend fun updateChunkProgress(chunkId: Long, currentByte: Long, status: Int) {
        chunkDao.updateChunkProgress(chunkId, currentByte, status)
    }

    suspend fun pauseAll() {
        downloadDao.updateAllStatus(DownloadStatus.RUNNING.code, DownloadStatus.PAUSED.code)
    }

    suspend fun resumeAll() {
        downloadDao.updateAllStatus(DownloadStatus.PAUSED.code, DownloadStatus.QUEUED.code)
    }
}
