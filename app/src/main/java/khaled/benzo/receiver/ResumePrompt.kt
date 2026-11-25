package khaled.benzo.receiver

import android.content.Context
import khaled.benzo.data.db.BenzoDatabase
import khaled.benzo.service.NotificationHelper

/** Posts the "tap to resume" notification when downloads are waiting and the service cannot be started from here. */
internal fun notifyIfDownloadsWaiting(context: Context) {
    val pending = kotlinx.coroutines.runBlocking {
        BenzoDatabase.getInstance(context).downloadDao().getPendingAndActiveDownloads().size
    }
    if (pending > 0) NotificationHelper(context).showResumePrompt(pending)
}
