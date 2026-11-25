package khaled.benzo.core.queue

import khaled.benzo.core.download.PermanentDownloadException
import khaled.benzo.core.download.RangeIgnoredException

/**
 * Decides whether a download that just failed is put back in the queue by itself ("auto-resume").
 * Only failures that can plausibly go away (dropped connection, timeout, server error) are retried, a limited number
 * of times with a growing delay, so a download can never loop forever.
 */
object AutoResumePolicy {
    const val MAX_AUTOMATIC_ATTEMPTS = 3
    private const val BASE_DELAY_MS = 30_000L

    fun shouldResume(enabled: Boolean, attemptsSoFar: Int, failure: Throwable?): Boolean =
        enabled &&
            attemptsSoFar < MAX_AUTOMATIC_ATTEMPTS &&
            failure !is PermanentDownloadException &&
            failure !is RangeIgnoredException &&
            failure?.message?.startsWith("Not enough free storage") != true

    /** 30 s, 60 s, 120 s for the first three automatic attempts. */
    fun delayMs(attemptsSoFar: Int): Long = BASE_DELAY_MS shl attemptsSoFar.coerceIn(0, 6)
}
