package khaled.benzo.core.download

import java.util.Locale

class SpeedTracker(private val windowSizeMs: Long = 2000L) {

    private data class Sample(val timestamp: Long, val bytes: Long)

    private val samples = ArrayDeque<Sample>()
    private var totalBytesTransferred: Long = 0L

    fun recordBytes(bytes: Long) {
        val now = System.currentTimeMillis()
        synchronized(samples) {
            samples.addLast(Sample(now, bytes))
            totalBytesTransferred += bytes
            prune(now)
        }
    }

    fun getSpeedBytesPerSec(): Long {
        val now = System.currentTimeMillis()
        synchronized(samples) {
            prune(now)
            if (samples.size < 2) return 0L
            val first = samples.first()
            val last = samples.last()
            val timeDiff = (last.timestamp - first.timestamp)
            if (timeDiff <= 0) return 0L

            val bytesDiff = samples.sumOf { it.bytes }
            return (bytesDiff * 1000L) / timeDiff
        }
    }

    private fun prune(now: Long) {
        while (samples.isNotEmpty() && now - samples.first().timestamp > windowSizeMs) {
            samples.removeFirst()
        }
    }

    companion object {
        fun formatSize(bytes: Long): String {
            if (bytes <= 0L) return "0 B"
            val kb = 1024.0
            val mb = kb * 1024.0
            val gb = mb * 1024.0

            return when {
                bytes >= gb -> String.format(Locale.US, "%.2f GB", bytes / gb)
                bytes >= mb -> String.format(Locale.US, "%.2f MB", bytes / mb)
                bytes >= kb -> String.format(Locale.US, "%.2f KB", bytes / kb)
                else -> "$bytes B"
            }
        }

        fun formatEta(remainingBytes: Long, speedBytesPerSec: Long): String {
            if (remainingBytes <= 0L || speedBytesPerSec <= 0L) return "--:--"
            val seconds = remainingBytes / speedBytesPerSec
            val h = seconds / 3600
            val m = (seconds % 3600) / 60
            val s = seconds % 60

            return if (h > 0) {
                String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
            } else {
                String.format(Locale.US, "%02d:%02d", m, s)
            }
        }
    }
}
