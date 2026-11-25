package khaled.benzo.service

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import khaled.benzo.core.queue.Conditions
import khaled.benzo.core.queue.ScheduleWindow
import khaled.benzo.data.repository.SettingsRepository
import java.util.Calendar

/**
 * Reads live network, battery and clock state and turns it into the pure [Conditions] the queue policy uses.
 * Battery state comes from the sticky ACTION_BATTERY_CHANGED broadcast, which works without a manifest receiver.
 */
class DeviceConditions(private val context: Context, private val settings: SettingsRepository) {

    fun read(activeCount: Int): Conditions {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        val hasNet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val unmetered = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true

        val battery: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level >= 0 && scale > 0) level * 100 / scale else -1
        val plugged = (battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0

        val now = Calendar.getInstance()
        val minute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val window = if (settings.scheduleEnabled) ScheduleWindow(settings.scheduleStartMinute, settings.scheduleStopMinute) else null

        return Conditions(
            networkAvailable = hasNet,
            onWifi = unmetered,
            batteryPercent = pct,
            charging = plugged,
            globalWifiOnly = settings.isWifiOnly,
            lowBatteryThreshold = if (settings.pauseOnBatteryLow) settings.batteryThresholdPercent else 0,
            nowMillis = System.currentTimeMillis(),
            scheduleStartMillis = null,
            activeCount = activeCount,
            maxConcurrent = settings.maxConcurrentDownloads,
            requireCharging = settings.stopOnChargerUnplug,
            scheduleWindow = window,
            minuteOfDay = minute
        )
    }
}
