package khaled.benzo.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import khaled.benzo.service.DownloadService

class DownloadActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, DownloadService::class.java).apply {
            action = intent.action
            putExtra(EXTRA_DOWNLOAD_ID, intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1L))
        }
        context.startService(serviceIntent)
    }

    companion object {
        const val ACTION_PAUSE = "khaled.benzo.ACTION_PAUSE"
        const val ACTION_RESUME = "khaled.benzo.ACTION_RESUME"
        const val ACTION_CANCEL = "khaled.benzo.ACTION_CANCEL"
        const val ACTION_EXIT_APP = "khaled.benzo.ACTION_EXIT_APP"
        const val EXTRA_DOWNLOAD_ID = "extra_download_id"
    }
}
