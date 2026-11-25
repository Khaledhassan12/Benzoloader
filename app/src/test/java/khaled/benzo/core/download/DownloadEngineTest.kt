package khaled.benzo.core.download

import okhttp3.mockwebserver.*
import okio.Buffer
import java.util.concurrent.TimeUnit
import khaled.benzo.data.model.DownloadChunk
import khaled.benzo.data.model.DownloadItem
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger

class DownloadEngineTest {
    private lateinit var server: MockWebServer
    private lateinit var dir: File
    private val data = ByteArray(3 * 1024 * 1024 + 12345) { ((it * 31 + it / 7) and 0xff).toByte() }
    private val flakyHits = AtomicInteger(0)
    private val rangeRequests = AtomicInteger(0)
    private fun url(p: String) = server.url(p).toString()

    private fun sha(b: ByteArray) = MessageDigest.getInstance("SHA-256").digest(b).toList()

    /** Builds a response like a real file server. */
    private fun respond(req: RecordedRequest, honorRange: Boolean, policy: SocketPolicy = SocketPolicy.KEEP_OPEN, throttle: Boolean = false): MockResponse {
        val range = req.getHeader("Range")
        var start = 0; var end = data.size - 1
        val r = MockResponse()
        if (honorRange && range != null) {
            rangeRequests.incrementAndGet()
            val m = Regex("bytes=(\\d+)-(\\d*)").find(range)!!
            start = m.groupValues[1].toInt()
            end = m.groupValues[2].ifEmpty { "${data.size - 1}" }.toInt().coerceAtMost(data.size - 1)
            r.setResponseCode(206).setHeader("Content-Range", "bytes $start-$end/${data.size}")
        } else r.setResponseCode(200)
        if (honorRange) r.setHeader("Accept-Ranges", "bytes")
        val body = Buffer().write(data, start, end - start + 1)
        r.setBody(body)
        r.socketPolicy = policy
        if (throttle) r.throttleBody(16 * 1024, 50, TimeUnit.MILLISECONDS)
        return r
    }

    @Before fun setUp() {
        dir = Files.createTempDirectory("dm-test").toFile()
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/range" -> respond(request, true)
                "/norange" -> respond(request, false)
                "/flaky" -> if (flakyHits.incrementAndGet() <= 3 && request.getHeader("Range") != "bytes=0-0")
                    respond(request, true, SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY) else respond(request, true)
                "/slow" -> respond(request, true, throttle = true)
                "/badlen" -> respond(request, true, SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY)
                else -> MockResponse().setResponseCode(404)
            }
        }
        server.start()
    }

    @After fun tearDown() { server.shutdown(); dir.deleteRecursively() }

    private fun item(path: String, threads: Int, name: String = "out.bin") =
        DownloadItem(id = 1, url = url(path), fileName = name, savePath = File(dir, name).absolutePath, threadsCount = threads)

    private suspend fun run(it: DownloadItem, existing: List<DownloadChunk> = emptyList(), limiter: TokenBucketLimiter = TokenBucketLimiter(0),
                            store: MutableList<DownloadChunk>? = null): Result<Unit> =
        DownloadEngine().runDownload(it, existing, limiter, emptyMap(),
            onChunkCreated = { store?.apply { clear(); addAll(it) }; it },
            onChunksSnapshot = { s -> store?.apply { clear(); addAll(s) } },
            onProgress = { _, _ -> })

    @Test fun singleConnectionDownloadIsByteExact() = runBlocking {
        assertTrue(run(item("/range", 1)).isSuccess)
        assertEquals(sha(data), sha(File(dir, "out.bin").readBytes()))
    }

    @Test fun multiConnectionDownloadIsByteExact() = runBlocking {
        rangeRequests.set(0)
        assertTrue(run(item("/range", 8)).isSuccess)
        assertEquals(sha(data), sha(File(dir, "out.bin").readBytes()))
        assertTrue("expected several ranged requests, got ${rangeRequests.get()}", rangeRequests.get() >= 8)
    }

    @Test fun sixteenThreadsIsAccepted() = runBlocking {
        assertTrue(run(item("/range", 16)).isSuccess)
        assertEquals(sha(data), sha(File(dir, "out.bin").readBytes()))
    }

    @Test fun serverWithoutRangeSupportFallsBackAndStaysCorrect() = runBlocking {
        // server ignores Range and always answers 200 with the whole file; the old engine corrupted the file here
        assertTrue(run(item("/norange", 8)).isSuccess)
        assertEquals(sha(data), sha(File(dir, "out.bin").readBytes()))
    }

    @Test fun interruptedConnectionsAreRetriedAndResumed() = runBlocking {
        flakyHits.set(0)
        assertTrue(run(item("/flaky", 4)).isSuccess)
        assertEquals(sha(data), sha(File(dir, "out.bin").readBytes()))
    }

    @Test fun truncatedBodyIsNeverReportedAsCompleted() = runBlocking {
        val r = DownloadEngine().runDownload(item("/badlen", 1), emptyList(), TokenBucketLimiter(0), emptyMap(),
            onChunkCreated = { it }, onProgress = { _, _ -> })
        // worker retries (server keeps cutting) and finally fails: must be failure, not success
        assertTrue(r.isFailure)
    }

    @Test fun httpErrorFailsWithoutRetryStorm() = runBlocking {
        val r = run(item("/missing", 4))
        assertTrue(r.isFailure)
    }

    @Test fun pauseThenResumeContinuesFromPersistedProgress() = runBlocking {
        val store = mutableListOf<DownloadChunk>()
        val job = launch(Dispatchers.IO) { runCatching { run(item("/slow", 4), store = store) } }
        delay(700)
        job.cancelAndJoin()                       // "pause"
        val saved = store.map { it.copy() }
        assertTrue("progress should have been persisted on pause", saved.sumOf { it.downloadedSize } > 0)
        assertTrue("download should not be complete yet", saved.sumOf { it.downloadedSize } < data.size)
        val r = run(item("/slow", 4), existing = saved, store = store)   // "resume"
        assertTrue(r.exceptionOrNull()?.toString() ?: "", r.isSuccess)
        assertEquals(sha(data), sha(File(dir, "out.bin").readBytes()))
    }

    @Test fun speedLimitReallyThrottlesTransfer() = runBlocking {
        val limit = 1_000_000L // bytes/s; the bucket starts with 1 s of burst, so ~3.1 MB must take >= ~2.1 s
        val t0 = System.nanoTime()
        val l = TokenBucketLimiter(0).also { it.setLimit(limit) }
        assertTrue(run(item("/range", 4), limiter = l).isSuccess)
        val secs = (System.nanoTime() - t0) / 1e9
        assertTrue("took only $secs s with a limit", secs >= 1.9)
        assertEquals(sha(data), sha(File(dir, "out.bin").readBytes()))
    }

    @Test fun twoGigabyteOffsetsUseLongMath() {
        val c = DownloadChunk(downloadId = 1, partIndex = 0, startByte = 2_147_483_648L, currentByte = 2_147_483_648L + 10, endByte = 4_294_967_295L)
        assertEquals(10L, c.downloadedSize)
        assertEquals(2_147_483_648L, c.totalSize)
    }

    @Test fun folderSavePathAndHostileNamesStayInsideTheFolder() = runBlocking {
        // the Editor passes the destination folder in savePath
        val folder = File(dir, "dl").also { it.mkdirs() }
        val it1 = DownloadItem(id = 1, url = url("/range"), fileName = "../../evil.bin", savePath = folder.absolutePath, threadsCount = 2)
        assertTrue(DownloadEngine().runDownload(it1, emptyList(), TokenBucketLimiter(0), emptyMap(), onChunkCreated = { it }, onProgress = { _, _ -> }).isSuccess)
        assertTrue(File(folder, "evil.bin").exists())
        assertFalse(File(dir, "evil.bin").exists())
        assertEquals(sha(data), sha(File(folder, "evil.bin").readBytes()))
    }

    @Test fun requestHeadersReachTheServer() = runBlocking {
        val r = DownloadEngine().runDownload(item("/range", 1), emptyList(), TokenBucketLimiter(0),
            mapOf("User-Agent" to "UA-Test", "Cookie" to "a=b"), onChunkCreated = { it }, onProgress = { _, _ -> })
        assertTrue(r.isSuccess)
        val seen = generateSequence { server.takeRequest(200, TimeUnit.MILLISECONDS) }.toList()
        assertTrue(seen.isNotEmpty() && seen.all { it.getHeader("User-Agent") == "UA-Test" && it.getHeader("Cookie") == "a=b" })
    }
}
