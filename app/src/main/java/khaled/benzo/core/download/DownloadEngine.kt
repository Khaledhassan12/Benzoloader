package khaled.benzo.core.download

import khaled.benzo.data.model.DownloadChunk
import khaled.benzo.data.model.DownloadItem
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
        val suggestedFileName: String?,
        val validator: Validator? = null
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
                ServerFileInfo(length, supportsRange, suggestedFileName,
                    Validator(response.header("ETag"), response.header("Last-Modified"), length))
            }
        }

    suspend fun runDownload(
        item: DownloadItem,
        existingChunks: List<DownloadChunk>,
        limiter: TokenBucketLimiter,
        headers: Map<String, String>,
        /** Persist (replace) the chunk plan and return the persisted chunks (with database ids). */
        onChunkCreated: suspend (List<DownloadChunk>) -> List<DownloadChunk>,
        /** Called about twice a second and once more when the job ends or is cancelled, so resume is exact. */
        onChunksSnapshot: suspend (List<DownloadChunk>) -> Unit = {},
        onProgress: suspend (downloadedBytes: Long, speed: Long) -> Unit,
        forceSingle: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val targetFile = resolveTarget(item)
        targetFile.parentFile?.mkdirs()

        // 1. Determine chunks (resume from persisted ones when present)
        var knownTotal = item.totalBytes
        val metaFile = File(targetFile.path + ".bzmeta")
        // Resuming: make sure the file on the server is still the one we started. A positive mismatch restarts clean.
        if (existingChunks.isNotEmpty() && !forceSingle) {
            val stored = Validator.parse(runCatching { metaFile.readText() }.getOrNull())
            val current = try {
                checkServer(item.url, headers).validator
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                null // offline or flaky: keep progress, the chunk workers retry on their own
            }
            if (!ResumePolicy.canResume(stored, current)) {
                targetFile.delete()
                metaFile.delete()
                return@withContext runDownload(item, emptyList(), limiter, headers, onChunkCreated, onChunksSnapshot, onProgress, forceSingle)
            }
        }
        val chunks: List<DownloadChunk> = if (existingChunks.isNotEmpty() && !forceSingle) {
            existingChunks
        } else {
            val serverInfo = try {
                checkServer(item.url, headers)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                ServerFileInfo(item.totalBytes, false, null)
            }
            val totalSize = if (serverInfo.contentLength > 0) serverInfo.contentLength else item.totalBytes
            knownTotal = totalSize
            val partsCount = if (forceSingle) 1
                else ChunkPlanner.partsFor(totalSize, item.threadsCount, serverInfo.supportsRange)
            serverInfo.validator?.takeIf { it.usable }?.let { runCatching { metaFile.writeText(it.copy(length = totalSize).serialize()) } }

            if (totalSize > 0) {
                val parent = targetFile.parentFile
                val needed = totalSize - (if (targetFile.exists()) minOf(targetFile.length(), totalSize) else 0L)
                if (parent != null && parent.usableSpace in 0 until needed) {
                    return@withContext Result.failure(java.io.IOException("Not enough free storage: need $needed bytes"))
                }
            }
            if (forceSingle && targetFile.exists()) targetFile.delete()

            val plan = ChunkPlanner.ranges(totalSize, partsCount).mapIndexed { i, r ->
                DownloadChunk(downloadId = item.id, partIndex = i, startByte = r.first, currentByte = r.first, endByte = r.last)
            }
            onChunkCreated(plan)
        }
        val totalSize = if (chunks.any { it.endByte < 0 }) -1L else chunks.maxOf { it.endByte } + 1
        if (totalSize > 0) {
            RandomAccessFile(targetFile, "rw").use { raf -> if (raf.length() != totalSize) raf.setLength(totalSize) }
        } else if (!targetFile.exists()) targetFile.createNewFile()
        val wholeFile = chunks.size == 1 && chunks[0].startByte == 0L

        // 2. Progress accounting (atomic; workers run on several threads)
        val downloaded = java.util.concurrent.atomic.AtomicLong(chunks.sumOf { it.downloadedSize })
        val speedTracker = SpeedTracker()
        val snapshot = { chunks.map { it.copy() } }

        try {
            coroutineScope {
                val ticker = launch {
                    while (true) {
                        delay(500)
                        onProgress(downloaded.get(), speedTracker.getSpeedBytesPerSec())
                        onChunksSnapshot(snapshot())
                    }
                }
                try {
                    chunks.map { chunk ->
                        async {
                            ChunkWorker(
                                client = client, url = item.url, chunk = chunk, outputFile = targetFile,
                                limiter = limiter, headers = headers, wholeFile = wholeFile,
                                onProgress = { _, bytes ->
                                    downloaded.addAndGet(bytes.toLong())
                                    speedTracker.recordBytes(bytes.toLong())
                                }
                            ).execute().getOrThrow()
                        }
                    }.awaitAll()
                } finally {
                    ticker.cancel()
                }
            }
        } catch (e: RangeIgnoredException) {
            withContext(NonCancellable) { onChunksSnapshot(snapshot()) }
            if (forceSingle) return@withContext Result.failure(e)
            // Server cannot do ranges: fall back to one plain connection, from scratch.
            return@withContext runDownload(item, emptyList(), limiter, headers, onChunkCreated, onChunksSnapshot, onProgress, forceSingle = true)
        } catch (e: kotlinx.coroutines.CancellationException) {
            withContext(NonCancellable) { onChunksSnapshot(snapshot()); onProgress(downloaded.get(), 0L) }
            throw e
        } catch (e: Exception) {
            withContext(NonCancellable) { onChunksSnapshot(snapshot()); onProgress(downloaded.get(), 0L) }
            return@withContext Result.failure(e)
        }

        // 3. Verify before reporting success
        withContext(NonCancellable) { onChunksSnapshot(snapshot()) }
        val written = chunks.sumOf { it.downloadedSize }
        if (totalSize > 0) {
            if (written != totalSize || targetFile.length() != totalSize) {
                return@withContext Result.failure(java.io.IOException(
                    "Verification failed: received $written of $totalSize bytes, file is ${targetFile.length()}"))
            }
        } else {
            RandomAccessFile(targetFile, "rw").use { it.setLength(written) }
        }
        metaFile.delete()
        onProgress(written, 0L)
        Result.success(Unit)
    }

    /**
     * The Editor stores the destination *folder* in savePath. Accept either a folder or a full file path,
     * sanitise the server/user supplied name and never let it escape the folder.
     */
    internal fun resolveTarget(item: DownloadItem): File {
        val base = File(item.savePath)
        if (!base.isDirectory && base.name == item.fileName) return base
        val safe = sanitizeFileName(item.fileName)
        val target = File(base, safe)
        require(target.canonicalFile.toPath().startsWith(base.canonicalFile.toPath())) { "Invalid file name" }
        return target
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
        fun sanitizeFileName(raw: String): String {
            var n = raw.substringAfterLast('/').substringAfterLast('\\').trim()
            n = n.replace(Regex("[\u0000-\u001f<>:\"|?*]"), "_").trim('.', ' ')
            return (if (n.isEmpty()) "download" else n).take(200)
        }

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
