package khaled.benzo.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import khaled.benzo.service.DownloadService

class ScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        khaled.benzo.service.ScheduleManager.rearm(context)
        val serviceIntent = Intent(context, DownloadService::class.java).apply {
            action = DownloadService.ACTION_PROCESS_QUEUE
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: RuntimeException) {
            // Background foreground-service start can be refused by the system (ForegroundServiceStartNotAllowedException
            // is a RuntimeException). Tell the user, so the queue is not silently stuck.
            val pending = goAsync()
            Thread {
                try { notifyIfDownloadsWaiting(context) } catch (e: Exception) { }
                finally { pending.finish() }
            }.start()
        }
    }
}
