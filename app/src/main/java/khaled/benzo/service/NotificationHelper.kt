package khaled.benzo.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import khaled.benzo.R
import khaled.benzo.core.download.SpeedTracker
import khaled.benzo.core.notify.LockScreenMode
import khaled.benzo.core.notify.NotifButton
import khaled.benzo.core.notify.NotificationPolicy
import khaled.benzo.data.model.DownloadItem
import khaled.benzo.receiver.DownloadActionReceiver
import khaled.benzo.ui.main.MainActivity

class NotificationHelper(private val context: Context) {

    private val settings by lazy { khaled.benzo.data.repository.SettingsRepository(context) }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val activeChannel = NotificationChannel(
                CHANNEL_ACTIVE,
                context.getString(R.string.notification_channel_active),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_channel_active_desc)
                setShowBadge(false)
            }

            val completedChannel = NotificationChannel(
                CHANNEL_COMPLETED,
                context.getString(R.string.notification_channel_completed),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_completed_desc)
                enableVibration(true)
            }

            val errorChannel = NotificationChannel(
                CHANNEL_ERROR,
                context.getString(R.string.notification_channel_error),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_error_desc)
            }

            notificationManager.createNotificationChannels(
                listOf(activeChannel, completedChannel, errorChannel)
            )
        }
    }

    fun buildForegroundNotification(
        activeTasks: List<DownloadItem>,
        aggregateSpeed: Long
    ): Notification {
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val exitIntent = Intent(context, DownloadActionReceiver::class.java).apply {
            action = DownloadActionReceiver.ACTION_EXIT_APP
        }
        val exitPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            exitIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseAllIntent = Intent(context, DownloadActionReceiver::class.java).apply {
            action = DownloadActionReceiver.ACTION_PAUSE_ALL
        }
        val pauseAllPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            pauseAllIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (activeTasks.isNotEmpty()) {
            if (activeTasks.size == 1) {
                activeTasks.first().fileName
            } else {
                "${context.getString(R.string.nav_downloading)} (${activeTasks.size})"
            }
        } else {
            context.getString(R.string.app_name)
        }

        val speedText = if (aggregateSpeed > 0L) {
            "${SpeedTracker.formatSize(aggregateSpeed)}/s"
        } else {
            context.getString(R.string.status_queued)
        }

        val totalDownloaded = activeTasks.sumOf { it.downloadedBytes }
        val totalBytes = activeTasks.sumOf { if (it.totalBytes > 0) it.totalBytes else 0L }
        val overallPercent = if (totalBytes > 0) {
            ((totalDownloaded.toDouble() / totalBytes.toDouble()) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }

        val contentText = if (totalBytes > 0) {
            "${SpeedTracker.formatSize(totalDownloaded)} / ${SpeedTracker.formatSize(totalBytes)} · $speedText"
        } else {
            speedText
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ACTIVE)
            .setSmallIcon(R.drawable.stat_start)
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(mainPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(
                when (NotificationPolicy.lockScreen(settings.notifOnLockScreen)) {
                    LockScreenMode.FULL -> NotificationCompat.VISIBILITY_PUBLIC
                    LockScreenMode.HIDE_CONTENT -> NotificationCompat.VISIBILITY_PRIVATE
                }
            )
        for (b in NotificationPolicy.buttons(settings.notifPauseAllButton)) {
            when (b) {
                NotifButton.PAUSE_ALL -> builder.addAction(R.drawable.stat_stop, context.getString(R.string.notification_pause_all), pauseAllPendingIntent)
                NotifButton.EXIT -> builder.addAction(R.drawable.stat_stop, context.getString(R.string.notification_exit), exitPendingIntent)
            }
        }

        if (totalBytes > 0) {
            builder.setProgress(100, overallPercent, false)
        } else if (activeTasks.isNotEmpty()) {
            builder.setProgress(100, 0, true)
        }

        return builder.build()
    }

    fun showCompletedNotification(item: DownloadItem) {
        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            val uri = Uri.parse(item.savePath)
            setDataAndType(uri, item.mimeType ?: "*/*")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            item.id.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_COMPLETED)
            .setSmallIcon(R.drawable.stat_success)
            .setContentTitle(item.fileName)
            .setContentText("${context.getString(R.string.status_completed)} (${SpeedTracker.formatSize(item.totalBytes)})")
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(item.id.toInt() + 10000, notification)
    }

    /**
     * Shown when the system does not let us start the download service on our own (after a reboot, or when a background
     * start is refused). Tapping it opens the app, which is an allowed way to start the service.
     */
    fun showResumePrompt(pendingCount: Int) {
        val open = PendingIntent.getActivity(
            context, 20001,
            Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(context, CHANNEL_COMPLETED)
            .setSmallIcon(R.drawable.stat_success)
            .setContentTitle(context.getString(R.string.notif_resume_title))
            .setContentText(context.getString(R.string.notif_resume_text, pendingCount))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(RESUME_PROMPT_ID, n)
    }

    /** Removes the finished-download notifications this app posted. The resume prompt and the ongoing one are left alone. */
    fun clearCompletedNotifications() {
        notificationManager.activeNotifications
            .filter { it.notification.channelId == CHANNEL_COMPLETED && it.id != RESUME_PROMPT_ID }
            .forEach { notificationManager.cancel(it.id) }
    }

    fun cancelResumePrompt() = notificationManager.cancel(RESUME_PROMPT_ID)

    @Suppress("DEPRECATION")
    fun vibrate(ms: Long) {
        if (ms <= 0L) return
        try {
            val v = if (Build.VERSION.SDK_INT >= 31)
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager).defaultVibrator
            else context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            v.vibrate(android.os.VibrationEffect.createOneShot(ms, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (e: Exception) {
            // No vibrator or not allowed: ignore
        }
    }

    /** Short on-screen message when a download finishes (Video 8 shows one). Plain system toast: the reference toast carries the app icon, ours does not yet. */
    fun showFinishedToast(fileName: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            android.widget.Toast.makeText(context, context.getString(R.string.toast_downloaded, fileName), android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun playSound(rawResId: Int) {
        try {
            val mediaPlayer = MediaPlayer.create(context, rawResId)
            mediaPlayer?.setOnCompletionListener { it.release() }
            mediaPlayer?.start()
        } catch (e: Exception) {
            // Ignore sound error
        }
    }

    companion object {
        const val NOTIFICATION_ID_FOREGROUND = 1001
        const val RESUME_PROMPT_ID = 20001
        const val CHANNEL_ACTIVE = "benzo_channel_active"
        const val CHANNEL_COMPLETED = "benzo_channel_completed"
        const val CHANNEL_ERROR = "benzo_channel_error"
    }
}
