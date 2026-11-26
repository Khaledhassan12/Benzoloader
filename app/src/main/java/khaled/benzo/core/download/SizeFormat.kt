package khaled.benzo.core.download

import java.util.Locale

/**
 * Compact size text. The reference Size dialog has four boxes in two pairs: letters "g m k" or "G M K", and "b" or "B"
 * (OBSERVED in the settings recording; the row value reads e.g. "G M K · B"). The reference code (read for behaviour only)
 * writes the number and the unit letter with no space, 1024 steps, and the "b" or "B" letter after the speed.
 * Bits: 1 = "g m k", 2 = "G M K", 4 = "b", 8 = "B". Default 1 + 8 is the reference's built-in default (the recording shows
 * a user-changed "G M K · B"). Whether sizes outside speeds also carry the b/B letter is INFERRED.
 */
object SizeFormat {
    const val LOWER_UNITS = 1
    const val UPPER_UNITS = 2
    const val LOWER_B = 4
    const val UPPER_B = 8
    const val DEFAULT = LOWER_UNITS or UPPER_B

    @Volatile var mask: Int = DEFAULT

    /** Exactly one of each pair. */
    fun sanitize(m: Int): Int {
        val units = if (m and UPPER_UNITS != 0 && m and LOWER_UNITS == 0) UPPER_UNITS else LOWER_UNITS
        val b = if (m and LOWER_B != 0 && m and UPPER_B == 0) LOWER_B else UPPER_B
        return units or b
    }

    fun unitsLowercase(m: Int = mask): Boolean = sanitize(m) and LOWER_UNITS != 0
    fun byteLetter(m: Int = mask): String = if (sanitize(m) and LOWER_B != 0) "b" else "B"

    private fun letter(l: String, lower: Boolean) = if (lower) l.lowercase(Locale.US) else l.uppercase(Locale.US)

    private fun twoDecimals(x: Double, u: String): String {
        val s = (Math.round(x * 100.0) / 100.0f).toString()
        return (if (s.indexOf('.') < s.length - 2) s else s + "0") + u
    }

    private fun oneDecimal(x: Double, u: String): String = (Math.round(x * 10.0) / 10.0f).toString() + u

    /** Number plus unit letter (K, M, G, T), no space. Steps and rounding follow the reference's own compact text. */
    fun compact(bytes: Long, unitsMask: Int = mask): String {
        val lower = unitsLowercase(unitsMask)
        val k = letter("K", lower); val m = letter("M", lower); val g = letter("G", lower); val t = letter("T", lower)
        val j1 = bytes / 1024; val j2 = j1 / 1024; val j3 = j2 / 1024; val j4 = j3 / 1024
        return when {
            bytes <= 0L -> "0$k"
            j4 > 99 -> "$j4$t"
            j4 > 9 -> oneDecimal(j3 / 1024.0, t)
            j3 > 999 -> twoDecimals(j3 / 1024.0, t)
            j3 > 99 -> "$j3$g"
            j3 > 9 -> oneDecimal(j2 / 1024.0, g)
            j2 > 999 -> twoDecimals(j2 / 1024.0, g)
            j2 > 99 -> "$j2$m"
            j2 > 9 -> oneDecimal(j1 / 1024.0, m)
            j1 > 999 -> twoDecimals(j1 / 1024.0, m)
            j1 > 99 -> "$j1$k"
            j1 > 9 -> oneDecimal(bytes / 1024.0, k)
            else -> twoDecimals(bytes / 1024.0, k)
        }
    }

    /** Size text for lists: compact number, unit letter, then the chosen byte letter. */
    fun format(bytes: Long, decimals: Int = 2, unitsMask: Int = mask): String =
        if (bytes <= 0L) "0" + byteLetter(unitsMask) else compact(bytes, unitsMask) + byteLetter(unitsMask)
}
