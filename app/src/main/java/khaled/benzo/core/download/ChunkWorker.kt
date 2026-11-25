package khaled.benzo.core.download

import khaled.benzo.data.model.DownloadChunk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import kotlin.coroutines.coroutineContext

/** The server answered a Range request with a full 200 body (or a wrong range); the file must not be written at an offset. */
class RangeIgnoredException(message: String) : IOException(message)

/** A failure that retrying cannot fix (4xx, bad Content-Range, ...). */
class PermanentDownloadException(message: String) : IOException(message)

/**
 * Downloads one byte range into [outputFile] at the right offsets.
 *
 * Safety rules:
 *  - a ranged request is only accepted as HTTP 206 whose Content-Range starts exactly at the requested byte;
 *  - an HTTP 200 reply is accepted only when this chunk is the whole file and starts at byte 0;
 *  - a short body (connection dropped early) is an error, never a completed chunk;
 *  - transient errors are retried with exponential backoff, resuming from the last byte actually written.
 */
class ChunkWorker(
    private val client: OkHttpClient,
    private val url: String,
    private val chunk: DownloadChunk,
    private val outputFile: File,
    private val limiter: TokenBucketLimiter,
    private val headers: Map<String, String>,
    private val wholeFile: Boolean = false,
    private val maxRetries: Int = 4,
    private val retryBaseDelayMs: Long = 400,
    private val onProgress: (chunk: DownloadChunk, bytesRead: Int) -> Unit
) {

    suspend fun execute(): Result<Unit> = withContext(Dispatchers.IO) {
        var attempt = 0
        while (true) {
            if (isDone()) {
                chunk.status = 2
                return@withContext Result.success(Unit)
            }
            try {
                chunk.status = 1
                runOnce()
                if (!isDone()) throw IOException("Connection ended early at byte ${chunk.currentByte}")
                chunk.status = 2
                return@withContext Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: RangeIgnoredException) {
                chunk.status = 3
                return@withContext Result.failure(e)
            } catch (e: PermanentDownloadException) {
                chunk.status = 3
                return@withContext Result.failure(e)
            } catch (e: IOException) {
                val hint = (e as? TransientHttpException)?.retryAfterMs ?: 0L
                // The server asked for a wait longer than we sit through here: stop, never retry before its time.
                // The failure keeps the hint, so auto-resume can wait for it (or give up if it is too long).
                if (hint > RetryAfter.IN_WORKER_MAX_MS || attempt >= maxRetries) {
                    chunk.status = 3
                    return@withContext Result.failure(e)
                }
                // The wait is a coroutine delay, so pausing or cancelling the download ends it at once.
                delay(maxOf(retryBaseDelayMs shl attempt, hint))
                attempt++
            }
        }
        @Suppress("UNREACHABLE_CODE")
        Result.success(Unit)
    }

    @Volatile private var streamEnded = false

    private fun isDone() = streamEnded || (chunk.endByte >= 0 && chunk.currentByte > chunk.endByte)

    private suspend fun runOnce() {
        val sized = chunk.endByte >= 0
        val req = Request.Builder().url(url)
        headers.forEach { (k, v) -> req.header(k, v) }
        val resuming = chunk.currentByte > chunk.startByte
        if (sized && (!wholeFile || resuming)) {
            req.header("Range", "bytes=${chunk.currentByte}-${chunk.endByte}")
        }
        client.newCall(req.build()).execute().use { resp ->
            val code = resp.code
            if (code == 416 && sized) {
                // Requested range not satisfiable: only OK when we really already have everything.
                if (isDone()) return
                throw PermanentDownloadException("Server rejected range (HTTP 416); file may have changed")
            }
            if (TransientStatus.isTransient(code)) {
                throw TransientHttpException(code, RetryAfter.parseMs(resp.header("Retry-After"), resp.headers.getDate("Date")?.time, System.currentTimeMillis()))
            }
            if (code in 400..499) throw PermanentDownloadException("HTTP $code")
            val ranged = sized && (!wholeFile || resuming)
            var writeFrom = chunk.currentByte
            when {
                code == 206 -> {
                    val cr = resp.header("Content-Range")
                    val start = cr?.let { Regex("bytes\\s+(\\d+)-").find(it)?.groupValues?.get(1)?.toLongOrNull() }
                    if (start == null || start != chunk.currentByte) {
                        throw PermanentDownloadException("Content-Range start ${start} != requested ${chunk.currentByte}")
                    }
                }
                code == 200 -> {
                    if (ranged) throw RangeIgnoredException("Server ignored the Range header (HTTP 200)")
                    if (resuming) throw RangeIgnoredException("Cannot resume without range support")
                    writeFrom = 0
                    chunk.currentByte = chunk.startByte // whole-file stream starts at 0
                }
                else -> throw PermanentDownloadException("Unexpected HTTP $code")
            }
            val body = resp.body ?: throw IOException("Empty response body")
            val buffer = ByteArray(16 * 1024)
            RandomAccessFile(outputFile, "rw").use { raf ->
                val channel = raf.channel
                body.byteStream().use { input ->
                    var pos = writeFrom
                    var unsynced = 0L
                    try {
                    while (true) {
                        coroutineContext.ensureActive()
                        val limit = if (chunk.endByte >= 0) minOf(buffer.size.toLong(), chunk.endByte - pos + 1).toInt() else buffer.size
                        if (limit <= 0) break
                        val read = input.read(buffer, 0, limit)
                        if (read == -1) break
                        limiter.acquire(read)
                        val bb = ByteBuffer.wrap(buffer, 0, read)
                        while (bb.hasRemaining()) pos += channel.write(bb, pos)
                        chunk.currentByte = pos
                        unsynced += read
                        // Progress is saved in the database while the bytes may still sit in the OS cache. After a power loss
                        // or system crash the database could be ahead of the disk, so flush regularly. A killed app is safe either way.
                        if (unsynced >= SYNC_EVERY_BYTES) { channel.force(false); chunk.durableByte = pos; unsynced = 0L }
                        onProgress(chunk, read)
                    }
                    } finally {
                        if (unsynced > 0L) runCatching { channel.force(false); chunk.durableByte = chunk.currentByte } // also on pause, cancel and failure
                    }
                }
            }
            // Unknown size: a clean end of stream means the file is complete.
            if (chunk.endByte < 0) streamEnded = true
        }
    }

    private companion object { const val SYNC_EVERY_BYTES = 8L * 1024 * 1024 }
}
