package khaled.benzo.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import khaled.benzo.R
import khaled.benzo.core.download.DownloadEngine
import khaled.benzo.core.download.TokenBucketLimiter
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

    private suspend fun processQueue() {
        val maxConcurrent = settings.maxConcurrentDownloads
        if (activeJobs.size >= maxConcurrent) return

        val pending = repository.getAllDownloads(null)
        val pendingList = mutableListOf<DownloadItem>()
        // Fetch pending items directly from DB
        val pendingEntities = khaled.benzo.data.db.BenzoDatabase.getInstance(this)
            .downloadDao().getPendingAndActiveDownloads()

        for (entity in pendingEntities) {
            if (activeJobs.size >= maxConcurrent) break
            val item = entity.toModel()
            if (item.status == DownloadStatus.QUEUED && !activeJobs.containsKey(item.id)) {
                startTask(item.id)
            }
        }

        updateNotification()
    }

    private suspend fun startTask(id: Long) {
        val item = repository.getDownload(id) ?: return
        if (activeJobs.containsKey(id)) return

        wakeLockManager.acquire()
        repository.updateStatusAndSpeed(id, DownloadStatus.RUNNING, 0L)
        activeItems[id] = item.copy(status = DownloadStatus.RUNNING)

        val job = serviceScope.launch {
            val existingChunks = repository.getChunks(id)
            val result = downloadEngine.runDownload(
                item = item,
                existingChunks = existingChunks,
                limiter = limiter,
                headers = emptyMap(),
                onChunkCreated = { chunks ->
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
                repository.updateStatusAndSpeed(id, DownloadStatus.ERROR, 0L)
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

    private fun pauseTask(id: Long) {
        val job = activeJobs.remove(id)
        job?.cancel()
        activeItems.remove(id)
        serviceScope.launch {
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
