package khaled.benzo.service

import khaled.benzo.core.queue.AutoResumePolicy
import khaled.benzo.data.model.DownloadStatus
import khaled.benzo.data.repository.DownloadRepository
import khaled.benzo.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Puts a failed download back in the queue after the delay [AutoResumePolicy] gives, only if the row still has the
 * status the failure gave it (so a user pause, restart or delete wins). Attempt counts live in memory only.
 */
class AutoResumeScheduler(
    private val scope: CoroutineScope,
    private val repository: DownloadRepository,
    private val settings: SettingsRepository
) {
    private val attempts = ConcurrentHashMap<Long, Int>()

    fun onSuccess(id: Long) { attempts.remove(id) }

    fun onFailure(id: Long, failedStatus: DownloadStatus, failure: Throwable?) {
        // With "pause after error" the failed row is Paused, which a user pause also produces: the two cannot be told
        // apart, so that setting wins and nothing is resumed automatically.
        if (failedStatus != DownloadStatus.ERROR) return
        val so = attempts[id] ?: 0
        if (!AutoResumePolicy.shouldResume(settings.autoResume, so, failure)) return
        attempts[id] = so + 1
        scope.launch {
            delay(AutoResumePolicy.delayMs(so, failure))
            if (repository.getDownload(id)?.status == failedStatus) {
                repository.updateStatusAndSpeed(id, DownloadStatus.QUEUED, 0L)
            }
        }
    }
}
