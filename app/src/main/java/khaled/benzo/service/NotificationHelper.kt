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
import khaled.benzo.data.model.DownloadItem
import khaled.benzo.receiver.DownloadActionReceiver
import khaled.benzo.ui.main.MainActivity

class NotificationHelper(private val context: Context) {

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
            .addAction(R.drawable.stat_stop, context.getString(R.string.notification_exit), exitPendingIntent)

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
        const val CHANNEL_ACTIVE = "benzo_channel_active"
        const val CHANNEL_COMPLETED = "benzo_channel_completed"
        const val CHANNEL_ERROR = "benzo_channel_error"
    }
}
