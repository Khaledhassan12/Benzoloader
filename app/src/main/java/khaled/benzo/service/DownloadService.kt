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
    private lateinit var httpRunner: HttpTaskRunner
    private lateinit var torrentRunner: TorrentTaskRunner
    private lateinit var autoResume: AutoResumeScheduler
    private lateinit var seedingMonitor: khaled.benzo.core.torrent.SeedingMonitor

    private val activeJobs = ConcurrentHashMap<Long, Job>()
    private val activeItems = ConcurrentHashMap<Long, DownloadItem>()

    override fun onCreate() {
        super.onCreate()
        repository = DownloadRepository(this)
        settings = SettingsRepository(this)
        wakeLockManager = WakeLockManager(this)
        notificationHelper = NotificationHelper(this)
        downloadEngine = DownloadEngine(DownloadEngine.createDefaultClient { settings.activeProxy() })
        limiter = TokenBucketLimiter(settings.speedLimitBytesPerSec)
        deviceConditions = DeviceConditions(this, settings)
        httpRunner = HttpTaskRunner(repository, downloadEngine, limiter) { khaled.benzo.data.repository.SettingsRepository(this).defaultUserAgent }
        torrentRunner = TorrentTaskRunner(repository, filesDir, { settings.torrentSequential }, { settings.torrentSeedAfterFinish }) { seedingMonitor.watch(it) }
        seedingMonitor = khaled.benzo.core.torrent.SeedingMonitor({ settings.seedRatioTenths }, { settings.seedMinutes }, { h ->
            khaled.benzo.core.torrent.TorrentEngine.orNull()?.getStats(h)?.let { khaled.benzo.core.torrent.SeedingMonitor.Snapshot(it.uploaded, it.total, it.seedingSeconds) }
        }, { h -> khaled.benzo.core.torrent.TorrentEngine.orNull()?.pauseTorrent(h) })
        autoResume = AutoResumeScheduler(serviceScope, repository, settings)
        ScheduleManager.rearm(this)

        val initialNotif = notificationHelper.buildForegroundNotification(emptyList(), 0L)
        startForeground(NotificationHelper.NOTIFICATION_ID_FOREGROUND, initialNotif)

        // Queue loop
        serviceScope.launch {
            while (isActive) {
                processQueue()
                seedingMonitor.check()
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
            DownloadActionReceiver.ACTION_PAUSE_ALL -> {
                // Queued downloads are paused too: otherwise the queue would start them the moment the running ones stop.
                serviceScope.launch {
                    repository.pauseAllByUser()
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
        khaled.benzo.core.torrent.TorrentEngine.orNull()?.configureNetwork(
            settings.torrentDht, settings.torrentConnections,
            khaled.benzo.core.torrent.TorrentNetworkPolicy.portToApply(settings.torrentFixedPort, settings.torrentPort), settings.torrentAnonymous, settings.torrentEncryption
        )
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
        QueueWaitState.publish(items.filter { it.status == DownloadStatus.QUEUED }.mapNotNull { q ->
            val r = QueuePolicy.blockReason(q, cond.copy(activeCount = running))
            if (r == khaled.benzo.core.queue.BlockReason.NONE) null else q.id to r
        }.toMap())
        for (item in QueuePolicy.nextToStart(items, cond.copy(activeCount = running), settings.newestFirst)) {
            if (!activeJobs.containsKey(item.id)) startTask(item.id)
        }

        updateNotification()
    }

    /** Stops the transfer but keeps it in the queue so it resumes by itself when conditions allow. Progress is already saved. */
    private suspend fun requeueTask(id: Long) {
        activeJobs.remove(id)?.cancel()
        activeItems.remove(id)
        torrentHashOf(id)?.let { khaled.benzo.core.torrent.TorrentEngine.orNull()?.pauseTorrent(it) }
        repository.updateStatusAndSpeed(id, DownloadStatus.QUEUED, 0L)
        if (activeJobs.isEmpty()) wakeLockManager.release()
    }

    private suspend fun startTask(id: Long) {
        val item = repository.getDownload(id) ?: return
        if (activeJobs.containsKey(id)) return
        if (item.needsReentry) {
            // Saved cookies or headers cannot be decrypted. Do not retry silently: show why, keep the stored values as they are.
            repository.updateDownload(item.copy(status = DownloadStatus.ERROR, speed = 0L,
                errorMessage = getString(R.string.secrets_reenter_download)))
            updateNotification()
            return
        }

        // The chosen folder is never changed here. If it cannot take files, the download stops with a clear reason.
        val verdict = khaled.benzo.core.storage.FolderCheck.check(item.savePath, khaled.benzo.core.storage.AndroidFolderFs())
        if (verdict != khaled.benzo.core.storage.FolderCheck.Verdict.OK) {
            repository.updateDownload(item.copy(status = DownloadStatus.ERROR, speed = 0L,
                errorMessage = getString(khaled.benzo.ui.common.FolderMessages.res(verdict))))
            updateNotification()
            return
        }

        wakeLockManager.acquire()
        repository.updateStatusAndSpeed(id, DownloadStatus.RUNNING, 0L)
        activeItems[id] = item.copy(status = DownloadStatus.RUNNING)

        if (item.isTorrent) { startTorrentTask(item); return }
        downloadEngine.maxRetries = settings.errorRetries
        downloadEngine.minChunkBytes = settings.minThreadKb * 1024L
        downloadEngine.preallocate = settings.createEmptyFile

        val job = serviceScope.launch {
            val result = httpRunner.run(item) { downloadedBytes, speed ->
                activeItems[id] = item.copy(downloadedBytes = downloadedBytes, speed = speed, status = DownloadStatus.RUNNING)
                updateNotification()
            }

            activeJobs.remove(id)
            activeItems.remove(id)

            val expected = item.expectedChecksum
            // Success is only reported after the check passed. Hashing runs on the IO dispatcher (this scope), never the main thread.
            // Applies to plain HTTP downloads only; torrents take the other path above and are never checked.
            val verdict = if (result.isSuccess && expected != null)
                khaled.benzo.core.files.ChecksumPolicy.check(downloadEngine.resolveTarget(repository.getDownload(id) ?: item), expected)
                else khaled.benzo.core.files.ChecksumPolicy.Result.MATCH
            val checksumBad = verdict != khaled.benzo.core.files.ChecksumPolicy.Result.MATCH
            if (checksumBad) {
                // The file is kept (never deleted); the user sees why it is not marked finished. No finish toast, sound, vibration or after-finish action.
                val fresh = repository.getDownload(id) ?: item
                repository.updateDownload(fresh.copy(status = DownloadStatus.ERROR, speed = 0L,
                    errorMessage = getString(if (verdict == khaled.benzo.core.files.ChecksumPolicy.Result.MISMATCH) R.string.checksum_mismatch else R.string.checksum_unreadable)))
                notificationHelper.playSound(R.raw.error)
            } else if (result.isSuccess) {
                repository.updateStatusAndSpeed(id, DownloadStatus.COMPLETED, 0L)
                val completedItem = repository.getDownload(id) ?: item
                notificationHelper.showCompletedNotification(completedItem)
                notificationHelper.showFinishedToast(completedItem.fileName)
                if (settings.soundOnComplete) {
                    notificationHelper.playSound(R.raw.done)
                }
                notificationHelper.vibrate(khaled.benzo.core.notify.VibrationPolicy.durationMs(settings.vibrateOnComplete, settings.vibrateSuccessMs))
                autoResume.onSuccess(id)
                if (exitIfFinished(justCompleted = true)) return@launch
            } else {
                val failedStatus = khaled.benzo.core.files.ErrorPolicy.statusAfterFailure(settings.pauseAfterError)
                repository.updateStatusAndSpeed(id, failedStatus, 0L)
                notificationHelper.playSound(R.raw.error)
                notificationHelper.vibrate(khaled.benzo.core.notify.VibrationPolicy.durationMs(settings.vibrateOnError, settings.vibrateErrorMs))
                autoResume.onFailure(id, failedStatus, result.exceptionOrNull())
            }

            if (activeJobs.isEmpty()) {
                wakeLockManager.release()
            }
            updateNotification()
        }

        activeJobs[id] = job
        updateNotification()
    }

    private suspend fun startTorrentTask(item: DownloadItem) {
        val id = item.id
        if (item.torrentInfoHash == null) {
            activeItems.remove(id)
            repository.updateStatusAndSpeed(id, khaled.benzo.core.files.ErrorPolicy.statusAfterFailure(settings.pauseAfterError), 0L)
            return
        }
        val job = serviceScope.launch {
            val ok = torrentRunner.run(item) { running ->
                activeItems[id] = running
                updateNotification()
            }
            activeJobs.remove(id)
            activeItems.remove(id)
            if (!ok) {
                repository.updateStatusAndSpeed(id, khaled.benzo.core.files.ErrorPolicy.statusAfterFailure(settings.pauseAfterError), 0L)
                notificationHelper.playSound(R.raw.error)
                notificationHelper.vibrate(khaled.benzo.core.notify.VibrationPolicy.durationMs(settings.vibrateOnError, settings.vibrateErrorMs))
            } else {
                repository.updateStatusAndSpeed(id, DownloadStatus.COMPLETED, 0L)
                (repository.getDownload(id) ?: item).let { notificationHelper.showCompletedNotification(it); notificationHelper.showFinishedToast(it.fileName) }
                if (settings.soundOnComplete) notificationHelper.playSound(R.raw.done)
                notificationHelper.vibrate(khaled.benzo.core.notify.VibrationPolicy.durationMs(settings.vibrateOnComplete, settings.vibrateSuccessMs))
                if (exitIfFinished(justCompleted = true)) return@launch
            }
            if (activeJobs.isEmpty()) wakeLockManager.release()
            updateNotification()
        }
        activeJobs[id] = job
        updateNotification()
    }

    private suspend fun torrentHashOf(id: Long): String? =
        runCatching { repository.getDownload(id) }.getOrNull()
            ?.takeIf { it.isTorrent }?.torrentInfoHash

    private fun pauseTask(id: Long) {
        val job = activeJobs.remove(id)
        job?.cancel()
        activeItems.remove(id)
        serviceScope.launch {
            torrentHashOf(id)?.let { khaled.benzo.core.torrent.TorrentEngine.orNull()?.pauseTorrent(it) }
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
            torrentHashOf(id)?.let { hash ->
                khaled.benzo.core.torrent.TorrentEngine.orNull()?.removeTorrent(hash, false)
                khaled.benzo.core.torrent.ResumeStore.deleteAllFor(java.io.File(filesDir, "torrents"), hash)
            }
            repository.deleteDownload(id)
            updateNotification()
        }
        if (activeJobs.isEmpty()) {
            wakeLockManager.release()
        }
    }

    private val topWindow by lazy { khaled.benzo.service.TopWindowOverlay(this) }
    private val topWindowStart = ConcurrentHashMap<Long, Long>()

    /** Shows the Top window strip for the first running item, or removes it. Which item shows with several is UNCLEAR in the reference. */
    private fun updateTopWindow(tasks: List<DownloadItem>) {
        val running = tasks.filter { it.status == DownloadStatus.RUNNING }.sortedBy { it.id }
        val ids = running.map { it.id }.toSet()
        topWindowStart.keys.retainAll(ids)
        val first = running.firstOrNull()
        if (first == null) { topWindow.hide(); return }
        val now = System.currentTimeMillis()
        val started = topWindowStart.getOrPut(first.id) { now }
        topWindow.update(
            settings.topWindowConfig(), running.size,
            khaled.benzo.core.overlay.TopWindowSample(first.downloadedBytes, first.totalBytes, first.speed, (now - started) / 1000L, first.fileName)
        )
    }

    private fun updateNotification() {
        val tasks = activeItems.values.toList()
        updateTopWindow(tasks)
        val totalSpeed = tasks.sumOf { it.speed }
        val notif = notificationHelper.buildForegroundNotification(tasks, totalSpeed)
        val notifManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notifManager.notify(NotificationHelper.NOTIFICATION_ID_FOREGROUND, notif)
    }

    /** "After finishing: exit". Counts what is still running, waiting to start (QUEUED) or seeding before it stops the service. */
    private suspend fun exitIfFinished(justCompleted: Boolean): Boolean {
        val mode = khaled.benzo.core.queue.AfterFinish.fromCode(settings.afterFinish)
        if (mode == khaled.benzo.core.queue.AfterFinish.NOTHING) return false
        val waiting = repository.getAllOnce().count { it.status == DownloadStatus.QUEUED }
        if (khaled.benzo.core.queue.AfterFinishPolicy.shouldExit(mode, justCompleted, activeJobs.size, waiting, seedingMonitor.watchedCount())) {
            stopAllAndExit(clearFinished = false)
            return true
        }
        return false
    }

    /** [clearFinished]: only for the user's own Exit; an automatic exit after finishing must leave the completion notice to be seen. */
    private fun stopAllAndExit(clearFinished: Boolean = true) {
        if (clearFinished && settings.clearNotifsOnExit) notificationHelper.clearCompletedNotifications()
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
        // Not on the main thread: the database writes must not block it. The service still stops right after them.
        val ids = activeJobs.keys.toList()
        serviceScope.launch {
            try { for (id in ids) requeueTask(id) }
            finally { android.os.Handler(android.os.Looper.getMainLooper()).post { stopAllAndExit() } }
        }
    }

    override fun onDestroy() {
        topWindow.hide()
        serviceScope.cancel()
        wakeLockManager.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START_DOWNLOAD = "khaled.benzo.ACTION_START_DOWNLOAD"
        const val ACTION_PROCESS_QUEUE = "khaled.benzo.ACTION_PROCESS_QUEUE"
    }
}
