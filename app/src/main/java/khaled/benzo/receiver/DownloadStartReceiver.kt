package khaled.benzo.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import khaled.benzo.service.DownloadService

class DownloadStartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val downloadId = intent.getLongExtra(DownloadActionReceiver.EXTRA_DOWNLOAD_ID, -1L)
        val serviceIntent = Intent(context, DownloadService::class.java).apply {
            action = DownloadService.ACTION_START_DOWNLOAD
            putExtra(DownloadActionReceiver.EXTRA_DOWNLOAD_ID, downloadId)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
