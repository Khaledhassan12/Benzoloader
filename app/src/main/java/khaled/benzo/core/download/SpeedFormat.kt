package khaled.benzo.core.download

import java.util.Locale

/**
 * Speed text on a list row, driven by four checkboxes (bits: 1 current speed, 2 average speed, 4 byte/sec, 8 bit/sec).
 * The four choices were OBSERVED in the reference Speed row. How they combine is INFERRED: current and average are
 * joined as "current (avg average)", byte and bit text are joined with a middle dot.
 */
object SpeedFormat {
    const val CURRENT = 1
    const val AVERAGE = 2
    const val BYTE = 4
    const val BIT = 8
    const val DEFAULT = CURRENT or BYTE

    fun sanitize(m: Int): Int {
        var v = m and 15
        if (v and (CURRENT or AVERAGE) == 0) v = v or CURRENT
        if (v and (BYTE or BIT) == 0) v = v or BYTE
        return v
    }

    fun bitsText(bytesPerSec: Long): String {
        val bits = bytesPerSec.coerceAtLeast(0L) * 8.0
        return when {
            bits >= 1e9 -> String.format(Locale.US, "%.1f Gb/s", bits / 1e9)
            bits >= 1e6 -> String.format(Locale.US, "%.1f Mb/s", bits / 1e6)
            bits >= 1e3 -> String.format(Locale.US, "%.1f Kb/s", bits / 1e3)
            else -> String.format(Locale.US, "%d b/s", bits.toLong())
        }
    }

    private fun one(bytesPerSec: Long, mask: Int): String {
        val parts = ArrayList<String>(2)
        if (mask and BYTE != 0) parts.add(SizeFormat.format(bytesPerSec, 1) + "/s")
        if (mask and BIT != 0) parts.add(bitsText(bytesPerSec))
        return parts.joinToString(" \u00B7 ")
    }

    /** [averageBytesPerSec] is null when no average is known yet. */
    fun text(currentBytesPerSec: Long, averageBytesPerSec: Long?, rawMask: Int): String {
        val m = sanitize(rawMask)
        val showCur = m and CURRENT != 0
        val showAvg = m and AVERAGE != 0 && averageBytesPerSec != null
        return when {
            showCur && showAvg -> one(currentBytesPerSec, m) + " (" + one(averageBytesPerSec!!, m) + ")"
            showAvg -> one(averageBytesPerSec!!, m)
            showCur -> one(currentBytesPerSec, m)
            else -> one(currentBytesPerSec, m)
        }
    }
}
