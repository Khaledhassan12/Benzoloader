package khaled.benzo.service

import khaled.benzo.core.download.DownloadEngine
import khaled.benzo.core.download.RequestHeaders
import khaled.benzo.core.download.TokenBucketLimiter
import khaled.benzo.data.model.DownloadItem
import khaled.benzo.data.repository.DownloadRepository

/** Runs one HTTP(S) download through the engine and keeps its chunks and progress in the database. No service state. */
class HttpTaskRunner(
    private val repository: DownloadRepository,
    private val engine: DownloadEngine,
    private val limiter: TokenBucketLimiter,
    private val defaultUserAgent: () -> String = { "" }
) {
    /** The global limit always applies (shared). A per-download limit adds its own bucket on top, chained to the global one: it can only slow that download further. */
    private fun itemLimiter(item: DownloadItem): TokenBucketLimiter {
        val own = item.speedLimitKBps * 1024L
        return if (own > 0L) TokenBucketLimiter(own, parent = limiter) else limiter
    }

    suspend fun run(item: DownloadItem, onProgress: suspend (downloadedBytes: Long, speed: Long) -> Unit): Result<Unit> {
        val id = item.id
        return engine.runDownload(
            item = item,
            existingChunks = repository.getChunks(id),
            limiter = itemLimiter(item),
            headers = RequestHeaders.from(item, defaultUserAgent()),
            onChunkCreated = { chunks ->
                repository.deleteChunks(id)
                repository.saveChunks(chunks)
                repository.getChunks(id)
            },
            onChunksSnapshot = { chunks -> repository.saveChunks(chunks) },
            onProgress = { downloadedBytes, speed ->
                repository.updateProgress(id, downloadedBytes, speed)
                onProgress(downloadedBytes, speed)
            }
        )
    }
}
