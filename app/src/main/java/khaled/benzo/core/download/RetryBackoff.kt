package khaled.benzo.core.download

/** Wait between two tries of a failing part. With up to 128 allowed errors, plain doubling would overflow and wait for days. */
object RetryBackoff {
    const val MAX_MS = 30_000L

    /** Doubling wait capped at [MAX_MS]. Safe for any attempt number. */
    fun waitMs(base: Long, attempt: Int): Long {
        if (base <= 0L) return 0L
        return minOf(base shl attempt.coerceIn(0, 20), MAX_MS)
    }
}
