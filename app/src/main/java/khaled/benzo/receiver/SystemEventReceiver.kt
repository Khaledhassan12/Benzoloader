package khaled.benzo.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import khaled.benzo.service.DownloadService
import khaled.benzo.service.ScheduleManager

/**
 * Boot handling. Battery and power rules are not handled here: Android does not deliver those broadcasts to
 * manifest receivers, so DownloadService reads the battery state itself on every queue tick (see DeviceConditions).
 */
class SystemEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ScheduleManager.rearm(context)
            val serviceIntent = Intent(context, DownloadService::class.java).apply {
                action = DownloadService.ACTION_PROCESS_QUEUE
            }
            context.startForegroundService(serviceIntent)
        }
    }
}
