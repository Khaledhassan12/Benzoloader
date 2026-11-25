package khaled.benzo.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import khaled.benzo.R
import khaled.benzo.core.download.DownloadEngine
import khaled.benzo.core.download.TokenBucketLimiter
import khaled.benzo.core.queue.QueuePolicy
import khaled.benzo.data.model.DownloadItem
import khaled.benzo.data.model.DownloadStatus
import khaled.benzo.data.repository.DownloadRepository
import khaled.benzo.data.repository.SettingsRepository
import khaled.benzo.receiver.DownloadActionReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

class DownloadService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repository: DownloadRepository
    private lateinit var settings: SettingsRepository
    private lateinit var wakeLockManager: WakeLockManager
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var downloadEngine: DownloadEngine
    private lateinit var limiter: TokenBucketLimiter
    private lateinit var deviceConditions: DeviceConditions

    private val activeJobs = ConcurrentHashMap<Long, Job>()
    private val activeItems = ConcurrentHashMap<Long, DownloadItem>()

    override fun onCreate() {
        super.onCreate()
        repository = DownloadRepository(this)
        settings = SettingsRepository(this)
        wakeLockManager = WakeLockManager(this)
        notificationHelper = NotificationHelper(this)
        downloadEngine = DownloadEngine()
        limiter = TokenBucketLimiter(settings.speedLimitBytesPerSec)
        deviceConditions = DeviceConditions(this, settings)
        ScheduleManager.rearm(this)

        val initialNotif = notificationHelper.buildForegroundNotification(emptyList(), 0L)
        startForeground(NotificationHelper.NOTIFICATION_ID_FOREGROUND, initialNotif)

        // Queue loop
        serviceScope.launch {
            while (isActive) {
                processQueue()
                delay(1000)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val downloadId = intent?.getLongExtra(DownloadActionReceiver.EXTRA_DOWNLOAD_ID, -1L) ?: -1L

        when (action) {
            ACTION_START_DOWNLOAD -> {
                if (downloadId != -1L) {
                    serviceScope.launch {
                        startTask(downloadId)
                    }
                }
            }
            DownloadActionReceiver.ACTION_PAUSE -> {
                if (downloadId != -1L) {
                    pauseTask(downloadId)
                }
            }
            DownloadActionReceiver.ACTION_RESUME -> {
                if (downloadId != -1L) {
                    serviceScope.launch {
                        startTask(downloadId)
                    }
                }
            }
            DownloadActionReceiver.ACTION_CANCEL -> {
                if (downloadId != -1L) {
                    cancelTask(downloadId)
                }
            }
            DownloadActionReceiver.ACTION_EXIT_APP -> {
                stopAllAndExit()
            }
            ACTION_PAUSE_ALL_BATTERY -> {
                serviceScope.launch {
                    repository.pauseAll()
                    activeJobs.keys.toList().forEach { pauseTask(it) }
                }
            }
            ACTION_PROCESS_QUEUE -> {
                serviceScope.launch {
                    processQueue()
                }
            }
        }

        return START_STICKY
    }

    /**
     * Applies the queue policy: running downloads that hit a blocking rule (no network, Wi-Fi only on mobile data,
     * low battery, unplugged, outside the schedule window) go back to the queue, and queued ones start when every rule
     * allows it. Downloads the user paused stay paused.
     */
    private var appliedLimit = Long.MIN_VALUE

    private suspend fun processQueue() {
        val limit = settings.speedLimitBytesPerSec
        if (limit != appliedLimit) {
            limiter.setLimit(limit)
            appliedLimit = limit
        }
        val entities = khaled.benzo.data.db.BenzoDatabase.getInstance(this)
            .downloadDao().getPendingAndActiveDownloads()
        val items = entities.map { it.toModel() }
        val cond = deviceConditions.read(activeJobs.size)

        for (id in activeJobs.keys.toList()) {
            val item = items.firstOrNull { it.id == id } ?: continue
            val reason = QueuePolicy.blockReason(item, cond.copy(activeCount = 0))
            if (reason.pausesRunning) requeueTask(id)
        }

        val running = activeJobs.size
        for (item in QueuePolicy.nextToStart(items, cond.copy(activeCount = running))) {
            if (!activeJobs.containsKey(item.id)) startTask(item.id)
        }

        updateNotification()
    }

    /** Stops the transfer but keeps it in the queue so it resumes by itself when conditions allow. Progress is already saved. */
    private suspend fun requeueTask(id: Long) {
        activeJobs.remove(id)?.cancel()
        activeItems.remove(id)
        torrentHashOf(id)?.let { khaled.benzo.core.torrent.TorrentEngine.instance.pauseTorrent(it) }
        repository.updateStatusAndSpeed(id, DownloadStatus.QUEUED, 0L)
        if (activeJobs.isEmpty()) wakeLockManager.release()
    }

    private suspend fun startTask(id: Long) {
        val item = repository.getDownload(id) ?: return
        if (activeJobs.containsKey(id)) return

        wakeLockManager.acquire()
        repository.updateStatusAndSpeed(id, DownloadStatus.RUNNING, 0L)
        activeItems[id] = item.copy(status = DownloadStatus.RUNNING)

        if (item.isTorrent) { startTorrentTask(item); return }
        downloadEngine.maxRetries = settings.errorRetries

        val job = serviceScope.launch {
            val existingChunks = repository.getChunks(id)
            val result = downloadEngine.runDownload(
                item = item,
                existingChunks = existingChunks,
                limiter = limiter,
                headers = buildHeaders(item),
                onChunkCreated = { chunks ->
                    repository.deleteChunks(id)
                    repository.saveChunks(chunks)
                    repository.getChunks(id)
                },
                onChunksSnapshot = { chunks ->
                    repository.saveChunks(chunks)
                },
                onProgress = { downloadedBytes, speed ->
                    repository.updateProgress(id, downloadedBytes, speed)
                    activeItems[id] = item.copy(
                        downloadedBytes = downloadedBytes,
                        speed = speed,
                        status = DownloadStatus.RUNNING
                    )
                    updateNotification()
                }
            )

            activeJobs.remove(id)
            activeItems.remove(id)

            if (result.isSuccess) {
                repository.updateStatusAndSpeed(id, DownloadStatus.COMPLETED, 0L)
                val completedItem = repository.getDownload(id) ?: item
                notificationHelper.showCompletedNotification(completedItem)
                if (settings.soundOnComplete) {
                    notificationHelper.playSound(R.raw.done)
                }
            } else {
                repository.updateStatusAndSpeed(id, khaled.benzo.core.files.ErrorPolicy.statusAfterFailure(settings.pauseAfterError), 0L)
                notificationHelper.playSound(R.raw.error)
            }

            if (activeJobs.isEmpty()) {
                wakeLockManager.release()
            }
            updateNotification()
        }

        activeJobs[id] = job
        updateNotification()
    }

    /** Drives one torrent through the libtorrent session and mirrors real stats into its download row. */
    private suspend fun startTorrentTask(item: DownloadItem) {
        val id = item.id
        val hash = item.torrentInfoHash
        if (hash == null) {
            activeItems.remove(id)
            repository.updateStatusAndSpeed(id, khaled.benzo.core.files.ErrorPolicy.statusAfterFailure(settings.pauseAfterError), 0L)
            return
        }
        val job = serviceScope.launch {
            var failed = false
            try {
                val engine = khaled.benzo.core.torrent.TorrentEngine.instance
                if (engine.hasTorrent(hash)) {
                    engine.resumeTorrent(hash)
                } else {
                    val dir = java.io.File(item.savePath)
                    val tdir = java.io.File(filesDir, "torrents")
                    val tf = java.io.File(tdir, "$hash.torrent")
                    if (tf.exists()) {
                        val prios = khaled.benzo.core.torrent.TorrentMapping
                            .decodePriorities(java.io.File(tdir, "$hash.prio").takeIf { it.exists() }?.readText())
                        engine.addTorrentFile(tf, dir, prios)
                    } else if (khaled.benzo.core.torrent.TorrentMapping.isMagnet(item.url)) {
                        if (engine.addMagnet(item.url, dir) == null) failed = true
                    } else failed = true
                }
                while (!failed && isActive) {
                    val st = engine.getStats(hash)
                    if (st == null) { failed = true; break }
                    repository.updateProgress(id, st.downloaded, st.downloadSpeed)
                    activeItems[id] = item.copy(downloadedBytes = st.downloaded, speed = st.downloadSpeed,
                        totalBytes = if (st.total > 0) st.total else item.totalBytes, status = DownloadStatus.RUNNING)
                    updateNotification()
                    if (st.isFinished) break
                    delay(1000)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Throwable) {
                failed = true
            }
            activeJobs.remove(id)
            activeItems.remove(id)
            if (failed) {
                repository.updateStatusAndSpeed(id, khaled.benzo.core.files.ErrorPolicy.statusAfterFailure(settings.pauseAfterError), 0L)
                notificationHelper.playSound(R.raw.error)
            } else {
                repository.updateStatusAndSpeed(id, DownloadStatus.COMPLETED, 0L)
                (repository.getDownload(id) ?: item).let { notificationHelper.showCompletedNotification(it) }
                if (settings.soundOnComplete) notificationHelper.playSound(R.raw.done)
            }
            if (activeJobs.isEmpty()) wakeLockManager.release()
            updateNotification()
        }
        activeJobs[id] = job
        updateNotification()
    }

    private fun torrentHashOf(id: Long): String? =
        runCatching { kotlinx.coroutines.runBlocking { repository.getDownload(id) } }.getOrNull()
            ?.takeIf { it.isTorrent }?.torrentInfoHash

    /** Editor fields (user agent, referer, cookies) must really reach the request. Never logged. */
    private fun buildHeaders(item: DownloadItem): Map<String, String> = buildMap {
        item.userAgent?.takeIf { it.isNotBlank() }?.let { put("User-Agent", it) }
        item.referer?.takeIf { it.isNotBlank() }?.let { put("Referer", it) }
        item.cookies?.takeIf { it.isNotBlank() }?.let { put("Cookie", it) }
    }

    private fun pauseTask(id: Long) {
        val job = activeJobs.remove(id)
        job?.cancel()
        activeItems.remove(id)
        serviceScope.launch {
            torrentHashOf(id)?.let { khaled.benzo.core.torrent.TorrentEngine.instance.pauseTorrent(it) }
            repository.updateStatusAndSpeed(id, DownloadStatus.PAUSED, 0L)
            updateNotification()
        }
        if (activeJobs.isEmpty()) {
            wakeLockManager.release()
        }
    }

    private fun cancelTask(id: Long) {
        val job = activeJobs.remove(id)
        job?.cancel()
        activeItems.remove(id)
        serviceScope.launch {
            torrentHashOf(id)?.let { khaled.benzo.core.torrent.TorrentEngine.instance.removeTorrent(it, false) }
            repository.deleteDownload(id)
            updateNotification()
        }
        if (activeJobs.isEmpty()) {
            wakeLockManager.release()
        }
    }

    private fun updateNotification() {
        val tasks = activeItems.values.toList()
        val totalSpeed = tasks.sumOf { it.speed }
        val notif = notificationHelper.buildForegroundNotification(tasks, totalSpeed)
        val notifManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notifManager.notify(NotificationHelper.NOTIFICATION_ID_FOREGROUND, notif)
    }

    private fun stopAllAndExit() {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        activeItems.clear()
        wakeLockManager.release()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Android 15+ limits dataSync foreground services to 6 hours per 24 hours and then calls this; the service must
     * stop within seconds. Running downloads go back to the queue (progress is saved) and the service stops.
     */
    override fun onTimeout(startId: Int, fgsType: Int) {
        runBlocking {
            for (id in activeJobs.keys.toList()) requeueTask(id)
        }
        stopAllAndExit()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        wakeLockManager.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START_DOWNLOAD = "khaled.benzo.ACTION_START_DOWNLOAD"
        const val ACTION_PROCESS_QUEUE = "khaled.benzo.ACTION_PROCESS_QUEUE"
        const val ACTION_PAUSE_ALL_BATTERY = "khaled.benzo.ACTION_PAUSE_ALL_BATTERY"
    }
}
