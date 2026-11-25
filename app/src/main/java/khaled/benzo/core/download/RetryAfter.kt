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

/** Reads the Retry-After header: whole seconds, or an HTTP date. The result is bounded so a server cannot stall us. */
object RetryAfter {
    const val MAX_WAIT_MS = 60_000L

    fun parseMs(value: String?, nowMs: Long): Long? {
        val v = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        v.toLongOrNull()?.let { return if (it < 0) null else (it * 1000).coerceAtMost(MAX_WAIT_MS) }
        return try {
            val fmt = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).apply { timeZone = TimeZone.getTimeZone("GMT") }
            val at = fmt.parse(v)?.time ?: return null
            (at - nowMs).coerceIn(0L, MAX_WAIT_MS)
        } catch (e: java.text.ParseException) { null }
    }
}
