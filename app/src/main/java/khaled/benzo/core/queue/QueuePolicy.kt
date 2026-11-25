package khaled.benzo.core.queue

import khaled.benzo.data.model.DownloadItem
import khaled.benzo.data.model.DownloadStatus

/** Why a queued download may not start right now. Pure logic so it is unit-testable without Android. */
enum class BlockReason { NONE, NO_NETWORK, WAITING_FOR_WIFI, LOW_BATTERY, NOT_CHARGING, NOT_YET_SCHEDULED, OUTSIDE_SCHEDULE, CONCURRENCY_LIMIT;
    /** True for reasons that should also pause a download that is already running (it goes back to the queue). */
    val pausesRunning: Boolean get() = this != NONE && this != CONCURRENCY_LIMIT
}

/** Daily window in minutes since midnight. start == stop means no restriction. Handles windows that cross midnight. */
data class ScheduleWindow(val startMinute: Int, val stopMinute: Int) {
    fun contains(minuteOfDay: Int): Boolean = when {
        startMinute == stopMinute -> true
        startMinute < stopMinute -> minuteOfDay in startMinute until stopMinute
        else -> minuteOfDay >= startMinute || minuteOfDay < stopMinute
    }

    /** Minutes from [minuteOfDay] until the window next opens or closes (always 1..1440). */
    fun minutesToNextBoundary(minuteOfDay: Int): Int {
        if (startMinute == stopMinute) return 1440
        fun until(target: Int) = ((target - minuteOfDay) % 1440 + 1440) % 1440
        val a = until(startMinute); val b = until(stopMinute)
        return listOf(a, b).filter { it > 0 }.minOrNull() ?: 1440
    }
}

data class Conditions(
    val networkAvailable: Boolean,
    val onWifi: Boolean,
    val batteryPercent: Int,
    val charging: Boolean,
    val globalWifiOnly: Boolean,
    val lowBatteryThreshold: Int,      // 0 disables the rule
    val nowMillis: Long,
    val scheduleStartMillis: Long?,    // null = no schedule
    val activeCount: Int,
    val maxConcurrent: Int,
    val requireCharging: Boolean = false,   // 'stop when unplugged' rule
    val scheduleWindow: ScheduleWindow? = null,
    val minuteOfDay: Int = 0
)

object QueuePolicy {
    fun blockReason(item: DownloadItem, c: Conditions): BlockReason = when {
        !c.networkAvailable -> BlockReason.NO_NETWORK
        (item.isWifiOnly || c.globalWifiOnly) && !c.onWifi -> BlockReason.WAITING_FOR_WIFI
        c.lowBatteryThreshold > 0 && !c.charging && c.batteryPercent in 0..c.lowBatteryThreshold -> BlockReason.LOW_BATTERY
        c.requireCharging && !c.charging -> BlockReason.NOT_CHARGING
        c.scheduleWindow != null && !c.scheduleWindow.contains(c.minuteOfDay) -> BlockReason.OUTSIDE_SCHEDULE
        c.scheduleStartMillis != null && c.nowMillis < c.scheduleStartMillis -> BlockReason.NOT_YET_SCHEDULED
        c.activeCount >= c.maxConcurrent -> BlockReason.CONCURRENCY_LIMIT
        else -> BlockReason.NONE
    }

    /** Next queued items to start, oldest first, respecting every rule and the concurrency limit. */
    fun nextToStart(queued: List<DownloadItem>, c: Conditions): List<DownloadItem> {
        var active = c.activeCount
        val out = mutableListOf<DownloadItem>()
        for (item in queued.filter { it.status == DownloadStatus.QUEUED }.sortedBy { it.createdDate }) {
            val r = blockReason(item, c.copy(activeCount = active))
            if (r == BlockReason.NONE) { out.add(item); active++ }
            else if (r == BlockReason.CONCURRENCY_LIMIT) break
        }
        return out
    }
}

enum class StatusFilter { ALL, ACTIVE, QUEUED, PAUSED, FAILED, COMPLETED }
enum class SortKey { TIME, NAME, SIZE, CATEGORY }

object ListLogic {
    fun matches(item: DownloadItem, f: StatusFilter) = when (f) {
        StatusFilter.ALL -> true
        StatusFilter.ACTIVE -> item.status == DownloadStatus.RUNNING || item.status == DownloadStatus.CONNECTING
        StatusFilter.QUEUED -> item.status == DownloadStatus.QUEUED
        StatusFilter.PAUSED -> item.status == DownloadStatus.PAUSED
        StatusFilter.FAILED -> item.status == DownloadStatus.ERROR
        StatusFilter.COMPLETED -> item.status == DownloadStatus.COMPLETED
    }

    fun apply(items: List<DownloadItem>, filter: StatusFilter, query: String, sort: SortKey, ascending: Boolean): List<DownloadItem> {
        val q = query.trim()
        val base = items.filter { matches(it, filter) && (q.isEmpty() || it.fileName.contains(q, ignoreCase = true)) }
        val sorted = when (sort) {
            SortKey.TIME -> base.sortedBy { it.createdDate }
            SortKey.NAME -> base.sortedBy { it.fileName.lowercase() }
            SortKey.SIZE -> base.sortedBy { it.totalBytes }
            SortKey.CATEGORY -> base.sortedWith(compareBy({ it.category.name }, { it.fileName.lowercase() }))
        }
        return if (ascending) sorted else sorted.reversed()
    }
}
