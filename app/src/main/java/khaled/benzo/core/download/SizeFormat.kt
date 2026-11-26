package khaled.benzo.core.download

import java.util.Locale

/**
 * Size text with a user-chosen set of units (bits: 1 = B, 2 = KB, 4 = MB, 8 = GB). The reference Size row offers
 * unit checkboxes (OBSERVED); how it picks among the ticked units is INFERRED here: the largest ticked unit that the
 * value reaches, otherwise the smallest ticked one. Terabytes are shown only while GB is ticked and the value reaches them.
 */
object SizeFormat {
    const val B = 1
    const val KB = 2
    const val MB = 4
    const val GB = 8
    const val ALL = 15

    @Volatile var mask: Int = ALL

    fun sanitize(m: Int): Int = (m and ALL).let { if (it == 0) ALL else it }

    fun format(bytes: Long, decimals: Int = 2, unitsMask: Int = mask): String {
        val m = sanitize(unitsMask)
        if (bytes <= 0L) return "0 " + (if (m and B != 0) "B" else smallestLabel(m))
        val steps = listOf(GB to ("GB" to 1073741824.0), MB to ("MB" to 1048576.0), KB to ("KB" to 1024.0), B to ("B" to 1.0))
        val ticked = steps.filter { m and it.first != 0 }
        val v = bytes.toDouble()
        if (m and GB != 0 && v >= 1099511627776.0) return String.format(Locale.US, "%.${decimals}f TB", v / 1099511627776.0)
        val pick = ticked.firstOrNull { v >= it.second.second } ?: ticked.last()
        val (label, size) = pick.second
        return if (size == 1.0) "$bytes B" else String.format(Locale.US, "%.${decimals}f %s", v / size, label)
    }

    private fun smallestLabel(m: Int): String = when {
        m and B != 0 -> "B"; m and KB != 0 -> "KB"; m and MB != 0 -> "MB"; else -> "GB"
    }
}
