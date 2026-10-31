package khaled.benzo.core.download

import khaled.benzo.data.model.DownloadChunk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile

class ChunkWorker(
    private val client: OkHttpClient,
    private val url: String,
    private val chunk: DownloadChunk,
    private val outputFile: File,
    private val limiter: TokenBucketLimiter,
    private val headers: Map<String, String>,
    private val onProgress: (chunk: DownloadChunk, bytesRead: Int) -> Unit
) {

    suspend fun execute(): Result<Unit> = withContext(Dispatchers.IO) {
        if (chunk.isFinished) {
            return@withContext Result.success(Unit)
        }

        val requestBuilder = Request.Builder().url(url)
        headers.forEach { (k, v) -> requestBuilder.header(k, v) }

        // Set Range header
        if (chunk.endByte > 0) {
            requestBuilder.header("Range", "bytes=${chunk.currentByte}-${chunk.endByte}")
        } else if (chunk.currentByte > 0) {
            requestBuilder.header("Range", "bytes=${chunk.currentByte}-")
        }

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful && response.code != 206 && response.code != 200) {
                    return@withContext Result.failure(Exception("HTTP error code: ${response.code}"))
                }

                val body = response.body ?: return@withContext Result.failure(Exception("Empty response body"))
                val buffer = ByteArray(16 * 1024) // 16 KB buffer

                RandomAccessFile(outputFile, "rw").use { raf ->
                    raf.seek(chunk.currentByte)
                    body.byteStream().use { input ->
                        var read = 0
                        while (coroutineContext.isActive && input.read(buffer).also { read = it } != -1) {
                            limiter.acquire(read)
                            raf.write(buffer, 0, read)
                            chunk.currentByte += read
                            onProgress(chunk, read)

                            if (chunk.endByte > 0 && chunk.currentByte > chunk.endByte) {
                                break
                            }
                        }
                    }
                }

                if (!coroutineContext.isActive) {
                    return@withContext Result.failure(Exception("Cancelled"))
                }

                chunk.status = 2 // Completed
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
