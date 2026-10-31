package khaled.benzo.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import khaled.benzo.service.DownloadService

class AppExitReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, DownloadService::class.java).apply {
            action = DownloadActionReceiver.ACTION_EXIT_APP
        }
        context.startService(serviceIntent)
    }
}
