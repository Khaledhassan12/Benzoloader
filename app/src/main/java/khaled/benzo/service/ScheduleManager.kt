package khaled.benzo.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import khaled.benzo.core.queue.ScheduleWindow
import khaled.benzo.data.repository.SettingsRepository
import khaled.benzo.receiver.ScheduleReceiver
import java.util.Calendar

/** Arms one inexact alarm at the next schedule boundary so the queue is re-evaluated even if the service was stopped. */
object ScheduleManager {
    private const val REQUEST_CODE = 4107

    fun rearm(context: Context) {
        val settings = SettingsRepository(context)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            context, REQUEST_CODE, Intent(context, ScheduleReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (!settings.scheduleEnabled) { am.cancel(pi); return }
        val now = Calendar.getInstance()
        val minute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val wait = ScheduleWindow(settings.scheduleStartMinute, settings.scheduleStopMinute).minutesToNextBoundary(minute)
        val triggerAt = now.timeInMillis - now.get(Calendar.SECOND) * 1000L - now.get(Calendar.MILLISECOND) + wait * 60_000L
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    }
}
