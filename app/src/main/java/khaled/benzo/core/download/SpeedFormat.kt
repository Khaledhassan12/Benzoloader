package khaled.benzo.core.download

/**
 * Speed text on a list row. The reference Speed dialog has four boxes in two pairs: "current speed" or "average speed",
 * and "byte/sec." or "bit/sec." (OBSERVED; its row value reads "current speed · byte/sec."). The pair is exclusive:
 * INFERRED from the row value always naming exactly one of each. Reference code (read for behaviour only): bit mode
 * multiplies by 8, then the compact size text and the b/B letter from the Size dialog are added with "/s".
 * Bits: 1 current, 2 average, 4 byte/sec, 8 bit/sec. Default 1 + 4 is the reference's default.
 */
object SpeedFormat {
    const val CURRENT = 1
    const val AVERAGE = 2
    const val BYTE = 4
    const val BIT = 8
    const val DEFAULT = CURRENT or BYTE

    fun sanitize(m: Int): Int {
        val a = if (m and AVERAGE != 0 && m and CURRENT == 0) AVERAGE else CURRENT
        val b = if (m and BIT != 0 && m and BYTE == 0) BIT else BYTE
        return a or b
    }

    /** [averageBytesPerSec] is null when no average is known yet. */
    fun text(currentBytesPerSec: Long, averageBytesPerSec: Long?, rawMask: Int, sizeMask: Int = SizeFormat.mask): String {
        val m = sanitize(rawMask)
        val v = (if (m and AVERAGE != 0) averageBytesPerSec ?: currentBytesPerSec else currentBytesPerSec).coerceAtLeast(0L)
        val x = if (m and BIT != 0) v * 8 else v
        return SizeFormat.compact(x, sizeMask) + SizeFormat.byteLetter(sizeMask) + "/s"
    }
}
