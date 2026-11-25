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
        c.lowBatteryThreshold > 0 && !c.charging && c.batteryPercent in 0..lowBatteryLimit(item, c) -> BlockReason.LOW_BATTERY
        c.requireCharging && !c.charging -> BlockReason.NOT_CHARGING
        c.scheduleWindow != null && !c.scheduleWindow.contains(c.minuteOfDay) -> BlockReason.OUTSIDE_SCHEDULE
        c.scheduleStartMillis != null && c.nowMillis < c.scheduleStartMillis -> BlockReason.NOT_YET_SCHEDULED
        c.activeCount >= c.maxConcurrent -> BlockReason.CONCURRENCY_LIMIT
        else -> BlockReason.NONE
    }

    /** Points above the threshold a waiting item needs before it starts again, so a download paused at the threshold does not restart and stop every percent. */
    const val LOW_BATTERY_RESUME_MARGIN = 5

    /** A running item stops at the threshold; a queued one starts again only above threshold + margin. */
    internal fun lowBatteryLimit(item: DownloadItem, c: Conditions): Int =
        if (item.status == DownloadStatus.QUEUED) restartBelowOrAt(c.lowBatteryThreshold) else c.lowBatteryThreshold

    /** Highest level (inclusive) at which a queued item still waits. Capped at 99 so a full battery always lets an unplugged restart happen. A threshold of 100 or more stays as the user set it. */
    fun restartBelowOrAt(threshold: Int): Int =
        if (threshold >= 99) threshold else minOf(threshold + LOW_BATTERY_RESUME_MARGIN, 99)

    /** Next queued items to start, oldest first (or newest first when [newestFirst], the "add to top" choice), respecting every rule and the concurrency limit. */
    fun nextToStart(queued: List<DownloadItem>, c: Conditions, newestFirst: Boolean = false): List<DownloadItem> {
        var active = c.activeCount
        val out = mutableListOf<DownloadItem>()
        val waiting = queued.filter { it.status == DownloadStatus.QUEUED }.sortedBy { it.createdDate }
        for (item in if (newestFirst) waiting.reversed() else waiting) {
            val r = blockReason(item, c.copy(activeCount = active))
            if (r == BlockReason.NONE) { out.add(item); active++ }
            else if (r == BlockReason.CONCURRENCY_LIMIT) break
        }
        return out
    }
}

/** UNFINISHED is the bottom bar's Queue: everything not completed (queued, connecting, running, paused, failed), so no pending work is hidden. */
enum class StatusFilter { ALL, ACTIVE, QUEUED, PAUSED, FAILED, COMPLETED, UNFINISHED }
/** ORDER is the order items were added (by id); the reference panel lists Order, Size, Name, Time and Category, each up and down. */
enum class SortKey { TIME, NAME, SIZE, CATEGORY, ORDER }

object ListLogic {
    /**
     * Category membership. OBSERVED in the reference video: the Torrents list holds rows of many file types and its count
     * (879 of 979) overlaps the type counts, so Torrents means "came from a torrent", not a file type of its own.
     * Every other category is the item's own type.
     */
    fun inCategory(item: DownloadItem, type: khaled.benzo.data.model.FileCategory): Boolean = when (type) {
        khaled.benzo.data.model.FileCategory.TORRENT -> item.isTorrent || item.category == khaled.benzo.data.model.FileCategory.TORRENT
        khaled.benzo.data.model.FileCategory.ALL -> true
        else -> item.category == type
    }

    fun matches(item: DownloadItem, f: StatusFilter) = when (f) {
        StatusFilter.ALL -> true
        StatusFilter.ACTIVE -> item.status == DownloadStatus.RUNNING || item.status == DownloadStatus.CONNECTING
        StatusFilter.QUEUED -> item.status == DownloadStatus.QUEUED
        StatusFilter.PAUSED -> item.status == DownloadStatus.PAUSED
        StatusFilter.FAILED -> item.status == DownloadStatus.ERROR
        StatusFilter.COMPLETED -> item.status == DownloadStatus.COMPLETED
        StatusFilter.UNFINISHED -> item.status != DownloadStatus.COMPLETED
    }

    fun apply(
        items: List<DownloadItem>, filter: StatusFilter, query: String, sort: SortKey, ascending: Boolean,
        time: TimeFilter = TimeFilter.ALL, nowMillis: Long = System.currentTimeMillis()
    ): List<DownloadItem> {
        val q = query.trim()
        val base = items.filter { matches(it, filter) && time.matches(it, nowMillis) && (q.isEmpty() || it.fileName.contains(q, ignoreCase = true)) }
        val sorted = when (sort) {
            SortKey.TIME -> base.sortedBy { it.createdDate }
            SortKey.NAME -> base.sortedBy { it.fileName.lowercase() }
            SortKey.SIZE -> base.sortedBy { it.totalBytes }
            SortKey.ORDER -> base.sortedBy { it.id }
            SortKey.CATEGORY -> base.sortedWith(compareBy({ it.category.name }, { it.fileName.lowercase() }))
        }
        return if (ascending) sorted else sorted.reversed()
    }
}
