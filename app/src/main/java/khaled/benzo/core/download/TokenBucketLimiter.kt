package khaled.benzo.core.download

import kotlinx.coroutines.delay

class TokenBucketLimiter(private var maxBytesPerSecond: Long = 0L) {

    private var availableTokens: Long = 0L
    private var lastRefillTime: Long = System.currentTimeMillis()

    fun setLimit(bytesPerSecond: Long) {
        synchronized(this) {
            maxBytesPerSecond = bytesPerSecond
            availableTokens = bytesPerSecond
            lastRefillTime = System.currentTimeMillis()
        }
    }

    suspend fun acquire(requested: Int) {
        if (maxBytesPerSecond <= 0L) return // Unlimited
        // A request larger than one second of budget could never be satisfied (the bucket is capped at the limit),
        // so charge at most the current limit; the delay below still paces the stream.
        val bytes = minOf(requested.toLong(), maxBytesPerSecond.coerceAtLeast(1L)).toInt()

        while (true) {
            var waitMs = 0L
            synchronized(this) {
                refill()
                if (availableTokens >= bytes) {
                    availableTokens -= bytes
                    return
                }
                val deficit = bytes - availableTokens
                waitMs = ((deficit.toDouble() / maxBytesPerSecond.toDouble()) * 1000.0).toLong().coerceAtLeast(10L)
            }
            delay(waitMs)
        }
    }

    private fun refill() {
        val now = System.currentTimeMillis()
        val elapsed = now - lastRefillTime
        if (elapsed > 0) {
            val tokensToAdd = (elapsed * maxBytesPerSecond) / 1000L
            availableTokens = (availableTokens + tokensToAdd).coerceAtMost(maxBytesPerSecond)
            lastRefillTime = now
        }
    }
}
