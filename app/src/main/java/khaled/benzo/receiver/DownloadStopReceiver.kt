package khaled.benzo.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import khaled.benzo.service.DownloadService

class DownloadStopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val downloadId = intent.getLongExtra(DownloadActionReceiver.EXTRA_DOWNLOAD_ID, -1L)
        val serviceIntent = Intent(context, DownloadService::class.java).apply {
            action = DownloadActionReceiver.ACTION_PAUSE
            putExtra(DownloadActionReceiver.EXTRA_DOWNLOAD_ID, downloadId)
        }
        context.startService(serviceIntent)
    }
}
