package khaled.benzo.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import khaled.benzo.service.ScheduleManager

/**
 * Boot handling. Battery and power rules are not handled here: Android does not deliver those broadcasts to
 * manifest receivers, so DownloadService reads the battery state itself on every queue tick (see DeviceConditions).
 */
class SystemEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Apps targeting Android 15+ may not start a dataSync foreground service from BOOT_COMPLETED
            // (developer.android.com/about/versions/15/changes/foreground-service-types), so only the schedule alarm is
            // re-armed here. Queued downloads continue when the app is opened or the next scheduled alarm fires.
            ScheduleManager.rearm(context)
        }
    }
}
