package khaled.benzo.ui.common

import android.content.Context
import khaled.benzo.R
import khaled.benzo.core.queue.BlockReason
import khaled.benzo.data.model.DownloadStatus

/** User-facing status words. A queued item shows the reason it is waiting when the queue loop knows it. */
object StatusText {
    fun of(context: Context, status: DownloadStatus, waiting: BlockReason?): String = when (status) {
        DownloadStatus.QUEUED -> when (waiting) {
            BlockReason.NO_NETWORK -> context.getString(R.string.wait_no_network)
            BlockReason.WAITING_FOR_WIFI -> context.getString(R.string.wait_wifi)
            BlockReason.LOW_BATTERY -> context.getString(R.string.wait_battery)
            BlockReason.NOT_CHARGING -> context.getString(R.string.wait_charger)
            BlockReason.NOT_YET_SCHEDULED, BlockReason.OUTSIDE_SCHEDULE -> context.getString(R.string.wait_schedule)
            BlockReason.CONCURRENCY_LIMIT -> context.getString(R.string.wait_slot)
            else -> context.getString(R.string.status_queued)
        }
        DownloadStatus.CONNECTING -> context.getString(R.string.status_connecting)
        DownloadStatus.RUNNING -> context.getString(R.string.status_downloading)
        DownloadStatus.PAUSED -> context.getString(R.string.status_paused)
        DownloadStatus.COMPLETED -> context.getString(R.string.status_completed)
        DownloadStatus.ERROR -> context.getString(R.string.status_error)
    }
}
