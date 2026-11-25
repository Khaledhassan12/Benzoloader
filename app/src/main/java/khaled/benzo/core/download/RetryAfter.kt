package khaled.benzo.core.download

import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** HTTP statuses that may pass by themselves: asking again later can work. */
object TransientStatus {
    fun isTransient(code: Int): Boolean = code == 408 || code == 429 || code >= 500
}

/** A transient HTTP status, with the server's own waiting hint if it sent one. */
class TransientHttpException(val code: Int, val retryAfterMs: Long?) : IOException("HTTP $code")

/**
 * Reads the Retry-After header: whole seconds, or an HTTP date. The value is NOT shortened: a server that asks for
 * a long wait gets that wait, or no automatic retry at all. Callers use [IN_WORKER_MAX_MS] and [AUTO_RESUME_MAX_MS]
 * to decide which of the two applies, and never retry earlier than the server asked.
 *
 * A date is compared with the server's own Date header when there is one (so a wrong phone clock does not matter),
 * and with the phone clock otherwise.
 */
object RetryAfter {
    /** Longest wait the chunk worker sits through inside one running download. */
    const val IN_WORKER_MAX_MS = 60_000L
    /** Longest server-requested wait after which auto-resume still retries. Longer asks stop automatic retry. */
    const val AUTO_RESUME_MAX_MS = 15 * 60_000L
    private const val SANITY_MAX_SECONDS = 366L * 24 * 3600

    fun parseMs(value: String?, serverDateMs: Long?, deviceNowMs: Long): Long? {
        val v = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        v.toLongOrNull()?.let { return if (it < 0) null else it.coerceAtMost(SANITY_MAX_SECONDS) * 1000 }
        return try {
            val fmt = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).apply { timeZone = TimeZone.getTimeZone("GMT") }
            val at = fmt.parse(v)?.time ?: return null
            val base = serverDateMs ?: deviceNowMs
            (at - base).coerceIn(0L, SANITY_MAX_SECONDS * 1000)
        } catch (e: java.text.ParseException) { null }
    }
}
