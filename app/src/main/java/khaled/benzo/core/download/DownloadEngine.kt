package khaled.benzo.core.download

import khaled.benzo.data.model.DownloadChunk
import khaled.benzo.data.model.DownloadItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit

class DownloadEngine(
    private val client: OkHttpClient = createDefaultClient()
) {

    data class ServerFileInfo(
        val contentLength: Long,
        val supportsRange: Boolean,
        val suggestedFileName: String?
    )

    suspend fun checkServer(url: String, headers: Map<String, String>): ServerFileInfo =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .header("Range", "bytes=0-0")
            headers.forEach { (k, v) -> request.header(k, v) }

            client.newCall(request.build()).execute().use { response ->
                val rangeHeader = response.header("Content-Range")
                val acceptRanges = response.header("Accept-Ranges")
                val disposition = response.header("Content-Disposition")

                val supportsRange = response.code == 206 ||
                        acceptRanges?.equals("bytes", ignoreCase = true) == true ||
                        rangeHeader != null

                var length = -1L
                if (rangeHeader != null) {
                    val slashIdx = rangeHeader.lastIndexOf('/')
                    if (slashIdx != -1) {
                        length = rangeHeader.substring(slashIdx + 1).trim().toLongOrNull() ?: -1L
                    }
                }
                if (length <= 0L) {
                    length = response.header("Content-Length")?.toLongOrNull() ?: -1L
                }

                val suggestedFileName = extractFileName(disposition, url)
                ServerFileInfo(length, supportsRange, suggestedFileName)
            }
        }

    suspend fun runDownload(
        item: DownloadItem,
        existingChunks: List<DownloadChunk>,
        limiter: TokenBucketLimiter,
        headers: Map<String, String>,
        onChunkCreated: suspend (List<DownloadChunk>) -> Unit,
        onProgress: suspend (downloadedBytes: Long, speed: Long) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val targetFile = File(item.savePath)
        targetFile.parentFile?.mkdirs()

        // 1. Determine Chunks
        val chunks: List<DownloadChunk> = if (existingChunks.isNotEmpty()) {
            existingChunks
        } else {
            val serverInfo = try {
                checkServer(item.url, headers)
            } catch (e: Exception) {
                ServerFileInfo(item.totalBytes, false, null)
            }

            val totalSize = if (serverInfo.contentLength > 0) serverInfo.contentLength else item.totalBytes
            val partsCount = if (serverInfo.supportsRange && totalSize > 0) item.threadsCount.coerceIn(1, 16) else 1

            if (totalSize > 0) {
                RandomAccessFile(targetFile, "rw").use { raf ->
                    raf.setLength(totalSize)
                }
            }

            val generatedChunks = mutableListOf<DownloadChunk>()
            if (partsCount > 1 && totalSize > 0) {
                val chunkSize = totalSize / partsCount
                for (i in 0 until partsCount) {
                    val start = i * chunkSize
                    val end = if (i == partsCount - 1) totalSize - 1 else (start + chunkSize - 1)
                    generatedChunks.add(
                        DownloadChunk(
                            downloadId = item.id,
                            partIndex = i,
                            startByte = start,
                            currentByte = start,
                            endByte = end,
                            status = 0
                        )
                    )
                }
            } else {
                generatedChunks.add(
                    DownloadChunk(
                        downloadId = item.id,
                        partIndex = 0,
                        startByte = 0L,
                        currentByte = 0L,
                        endByte = if (totalSize > 0) totalSize - 1 else -1L,
                        status = 0
                    )
                )
            }
            onChunkCreated(generatedChunks)
            generatedChunks
        }

        // 2. Setup Speed Tracker & Progress
        val speedTracker = SpeedTracker()
        var lastDbUpdateTime = System.currentTimeMillis()

        // 3. Launch Workers in Parallel
        try {
            coroutineScope {
                val tasks = chunks.map { chunk ->
                    async {
                        val worker = ChunkWorker(
                            client = client,
                            url = item.url,
                            chunk = chunk,
                            outputFile = targetFile,
                            limiter = limiter,
                            headers = headers,
                            onProgress = { c, bytes ->
                                speedTracker.recordBytes(bytes.toLong())
                                val now = System.currentTimeMillis()
                                if (now - lastDbUpdateTime >= 500) {
                                    lastDbUpdateTime = now
                                    val totalDownloaded = chunks.sumOf { it.downloadedSize }
                                    val currentSpeed = speedTracker.getSpeedBytesPerSec()
                                    this@coroutineScope.async {
                                        onProgress(totalDownloaded, currentSpeed)
                                    }
                                }
                            }
                        )
                        worker.execute()
                    }
                }
                val results = tasks.awaitAll()
                val failure = results.firstOrNull { it.isFailure }
                if (failure != null) {
                    return@coroutineScope Result.failure(failure.exceptionOrNull() ?: Exception("Chunk download failed"))
                }
                val totalDownloaded = chunks.sumOf { it.downloadedSize }
                onProgress(totalDownloaded, 0L)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractFileName(contentDisposition: String?, url: String): String? {
        if (!contentDisposition.isNullOrBlank()) {
            val patterns = listOf("filename*=", "filename=")
            for (p in patterns) {
                val idx = contentDisposition.indexOf(p)
                if (idx != -1) {
                    var fn = contentDisposition.substring(idx + p.length).trim()
                    if (fn.startsWith("UTF-8''", ignoreCase = true)) {
                        fn = fn.substring(7)
                    }
                    fn = fn.trim('"', ';', ' ')
                    if (fn.isNotEmpty()) return fn
                }
            }
        }
        val cleanUrl = url.substringBefore('?').substringBefore('#')
        val lastSlash = cleanUrl.lastIndexOf('/')
        return if (lastSlash != -1 && lastSlash < cleanUrl.length - 1) {
            cleanUrl.substring(lastSlash + 1)
        } else {
            null
        }
    }

    companion object {
        fun createDefaultClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        }
    }
}
