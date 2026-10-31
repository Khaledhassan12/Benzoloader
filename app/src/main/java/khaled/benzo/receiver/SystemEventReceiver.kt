package khaled.benzo.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import khaled.benzo.data.repository.SettingsRepository
import khaled.benzo.service.DownloadService

class SystemEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val settings = SettingsRepository(context)

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                // Auto-start downloads on boot if pending
                val serviceIntent = Intent(context, DownloadService::class.java).apply {
                    action = DownloadService.ACTION_PROCESS_QUEUE
                }
                context.startForegroundService(serviceIntent)
            }

            Intent.ACTION_BATTERY_CHANGED -> {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    val batteryPct = (level * 100) / scale
                    val threshold = settings.batteryThresholdPercent
                    if (batteryPct <= threshold) {
                        // Pause all active downloads to preserve battery
                        val pauseIntent = Intent(context, DownloadService::class.java).apply {
                            action = DownloadService.ACTION_PAUSE_ALL_BATTERY
                        }
                        context.startService(pauseIntent)
                    }
                }
            }

            Intent.ACTION_POWER_DISCONNECTED -> {
                if (settings.stopOnChargerUnplug) {
                    val pauseIntent = Intent(context, DownloadService::class.java).apply {
                        action = DownloadService.ACTION_PAUSE_ALL_BATTERY
                    }
                    context.startService(pauseIntent)
                }
            }
        }
    }
}
