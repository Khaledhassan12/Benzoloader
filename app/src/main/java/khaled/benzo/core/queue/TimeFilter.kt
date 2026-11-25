package khaled.benzo.core.queue

import khaled.benzo.data.model.DownloadItem
import java.time.Instant
import java.time.ZoneId

/**
 * Time groups of the list filter. OBSERVED in the reference panel: All, Today, Yesterday, Week, Month, Year, with counts
 * that nest (Week 5 inside Month 37 inside Year 947), so Week, Month and Year are rolling windows, not calendar buckets.
 * INFERRED: window lengths of 7, 30 and 365 days counted back from the start of today, and the date used is the finish
 * time for a completed item and the add time otherwise.
 */
enum class TimeFilter(val days: Int) {
    ALL(-1), TODAY(0), YESTERDAY(1), WEEK(7), MONTH(30), YEAR(365);

    fun matches(item: DownloadItem, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        if (this == ALL) return true
        val stamp = if (item.completedDate > 0L) item.completedDate else item.createdDate
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val day = Instant.ofEpochMilli(stamp).atZone(zone).toLocalDate()
        val age = java.time.temporal.ChronoUnit.DAYS.between(day, today)
        return when (this) {
            TODAY -> age == 0L
            YESTERDAY -> age == 1L
            else -> age in 0 until days.toLong()
        }
    }

    companion object {
        /** Counts for the panel: a zero count is left out of the map, as the reference shows no number for it. */
        fun counts(items: List<DownloadItem>, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Map<TimeFilter, Int> =
            values().associateWith { f -> items.count { f.matches(it, nowMillis, zone) } }.filterValues { it > 0 }
    }
}
