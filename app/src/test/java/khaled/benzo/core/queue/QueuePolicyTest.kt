package khaled.benzo.core.queue

import khaled.benzo.data.model.DownloadItem
import khaled.benzo.data.model.DownloadStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class QueuePolicyTest {
    private fun it(id: Long, name: String = "f$id", st: DownloadStatus = DownloadStatus.QUEUED, wifi: Boolean = false, size: Long = id * 10, created: Long = id) =
        DownloadItem(id = id, url = "http://x/$name", fileName = name, savePath = "/tmp", status = st, isWifiOnly = wifi, totalBytes = size, createdDate = created)

    private val ok = Conditions(true, true, 80, false, false, 15, 1_000, null, 0, 2)

    @Test fun newestFirstReversesStartOrder() {
        val q = listOf(it(1), it(2), it(3))
        assertEquals(listOf(1L, 2L), QueuePolicy.nextToStart(q, ok).map { it.id })
        assertEquals(listOf(3L, 2L), QueuePolicy.nextToStart(q, ok, newestFirst = true).map { it.id })
    }
    @Test fun startsWhenAllConditionsMet() = assertEquals(BlockReason.NONE, QueuePolicy.blockReason(it(1), ok))
    @Test fun noNetworkBlocks() = assertEquals(BlockReason.NO_NETWORK, QueuePolicy.blockReason(it(1), ok.copy(networkAvailable = false)))
    @Test fun perItemWifiOnlyWaitsOnMobileData() = assertEquals(BlockReason.WAITING_FOR_WIFI, QueuePolicy.blockReason(it(1, wifi = true), ok.copy(onWifi = false)))
    @Test fun globalWifiOnlyWaitsOnMobileData() = assertEquals(BlockReason.WAITING_FOR_WIFI, QueuePolicy.blockReason(it(1), ok.copy(onWifi = false, globalWifiOnly = true)))
    @Test fun mobileDataAllowedWhenNotWifiOnly() = assertEquals(BlockReason.NONE, QueuePolicy.blockReason(it(1), ok.copy(onWifi = false)))
    @Test fun lowBatteryBlocksUnlessCharging() {
        assertEquals(BlockReason.LOW_BATTERY, QueuePolicy.blockReason(it(1), ok.copy(batteryPercent = 10)))
        assertEquals(BlockReason.NONE, QueuePolicy.blockReason(it(1), ok.copy(batteryPercent = 10, charging = true)))
        assertEquals(BlockReason.NONE, QueuePolicy.blockReason(it(1), ok.copy(batteryPercent = 10, lowBatteryThreshold = 0)))
    }
    @Test fun unpluggedBlocksOnlyWhenRuleEnabled() {
        assertEquals(BlockReason.NOT_CHARGING, QueuePolicy.blockReason(it(1), ok.copy(requireCharging = true, charging = false)))
        assertEquals(BlockReason.NONE, QueuePolicy.blockReason(it(1), ok.copy(requireCharging = true, charging = true)))
        assertEquals(BlockReason.NONE, QueuePolicy.blockReason(it(1), ok.copy(requireCharging = false, charging = false)))
    }
    @Test fun scheduleWindowSameDay() {
        val w = ScheduleWindow(22 * 60, 23 * 60)
        assertEquals(BlockReason.OUTSIDE_SCHEDULE, QueuePolicy.blockReason(it(1), ok.copy(scheduleWindow = w, minuteOfDay = 21 * 60)))
        assertEquals(BlockReason.NONE, QueuePolicy.blockReason(it(1), ok.copy(scheduleWindow = w, minuteOfDay = 22 * 60)))
        assertEquals(BlockReason.OUTSIDE_SCHEDULE, QueuePolicy.blockReason(it(1), ok.copy(scheduleWindow = w, minuteOfDay = 23 * 60)))
    }
    @Test fun scheduleWindowCrossingMidnight() {
        val w = ScheduleWindow(23 * 60, 6 * 60)
        assertEquals(true, w.contains(23 * 60 + 30)); assertEquals(true, w.contains(0)); assertEquals(true, w.contains(5 * 60 + 59))
        assertEquals(false, w.contains(6 * 60)); assertEquals(false, w.contains(12 * 60))
    }
    @Test fun equalStartAndStopMeansNoRestriction() = assertEquals(true, ScheduleWindow(0, 0).contains(777))
    @Test fun nextBoundaryMinutes() {
        val w = ScheduleWindow(23 * 60, 6 * 60)
        assertEquals(60, w.minutesToNextBoundary(22 * 60))      // opens in an hour
        assertEquals(360, w.minutesToNextBoundary(0))            // closes at 06:00
        assertEquals(1, w.minutesToNextBoundary(6 * 60 - 1))
        assertEquals(1, w.minutesToNextBoundary(23 * 60 - 1))
        assertEquals(1440, ScheduleWindow(5, 5).minutesToNextBoundary(10))
    }
    @Test fun pausesRunningOnlyForEnvironmentReasons() {
        assertEquals(true, BlockReason.WAITING_FOR_WIFI.pausesRunning)
        assertEquals(true, BlockReason.LOW_BATTERY.pausesRunning)
        assertEquals(true, BlockReason.OUTSIDE_SCHEDULE.pausesRunning)
        assertEquals(false, BlockReason.CONCURRENCY_LIMIT.pausesRunning)
        assertEquals(false, BlockReason.NONE.pausesRunning)
    }
    @Test fun scheduleWaitsForStartTime() {
        assertEquals(BlockReason.NOT_YET_SCHEDULED, QueuePolicy.blockReason(it(1), ok.copy(scheduleStartMillis = 5_000)))
        assertEquals(BlockReason.NONE, QueuePolicy.blockReason(it(1), ok.copy(scheduleStartMillis = 500)))
    }
    @Test fun concurrencyLimitIsRespectedAndOldestFirst() {
        val q = listOf(it(3), it(1), it(2))
        assertEquals(listOf(1L, 2L), QueuePolicy.nextToStart(q, ok).map { it.id })
        assertEquals(listOf(1L), QueuePolicy.nextToStart(q, ok.copy(activeCount = 1)).map { it.id })
        assertEquals(emptyList<Long>(), QueuePolicy.nextToStart(q, ok.copy(activeCount = 2)).map { it.id })
    }
    @Test fun wifiOnlyItemDoesNotBlockOthersInQueue() {
        val q = listOf(it(1, wifi = true), it(2))
        assertEquals(listOf(2L), QueuePolicy.nextToStart(q, ok.copy(onWifi = false)).map { it.id })
    }

    private val items = listOf(
        it(1, "b.zip", DownloadStatus.COMPLETED, size = 50), it(2, "A.mp4", DownloadStatus.RUNNING, size = 900),
        it(3, "c.pdf", DownloadStatus.ERROR, size = 5), it(4, "d.mp3", DownloadStatus.PAUSED, size = 70), it(5, "e.apk", DownloadStatus.QUEUED, size = 300))
    @Test fun filtersByStatus() {
        assertEquals(listOf(2L), ListLogic.apply(items, StatusFilter.ACTIVE, "", SortKey.TIME, true).map { it.id })
        assertEquals(listOf(3L), ListLogic.apply(items, StatusFilter.FAILED, "", SortKey.TIME, true).map { it.id })
        assertEquals(5, ListLogic.apply(items, StatusFilter.ALL, "", SortKey.TIME, true).size)
    }
    @Test fun unfinishedKeepsEverythingButCompleted() {
        val u = ListLogic.apply(items, StatusFilter.UNFINISHED, "", SortKey.TIME, true)
        assertEquals(true, u.none { it.status == DownloadStatus.COMPLETED })
        assertEquals(items.size - ListLogic.apply(items, StatusFilter.COMPLETED, "", SortKey.TIME, true).size, u.size)
    }
    @Test fun searchIsCaseInsensitive() = assertEquals(listOf(2L), ListLogic.apply(items, StatusFilter.ALL, "a.MP", SortKey.TIME, true).map { it.id })
    @Test fun sortsByNameSizeAndDirection() {
        assertEquals(listOf(2L, 1L, 3L, 4L, 5L), ListLogic.apply(items, StatusFilter.ALL, "", SortKey.NAME, true).map { it.id })
        assertEquals(listOf(2L, 5L, 4L, 1L, 3L), ListLogic.apply(items, StatusFilter.ALL, "", SortKey.SIZE, false).map { it.id })
    }

    @Test fun queuedItemWaitsUntilAboveThresholdPlusMargin() {
        val low = ok.copy(lowBatteryThreshold = 15, charging = false)
        assertEquals(BlockReason.LOW_BATTERY, QueuePolicy.blockReason(it(1), low.copy(batteryPercent = 20)))
        assertEquals(BlockReason.NONE, QueuePolicy.blockReason(it(1), low.copy(batteryPercent = 21)))
    }

    @Test fun runningItemStopsOnlyAtThreshold() {
        val low = ok.copy(lowBatteryThreshold = 15, charging = false)
        assertEquals(BlockReason.NONE, QueuePolicy.blockReason(it(1, st = DownloadStatus.RUNNING), low.copy(batteryPercent = 16)))
        assertEquals(BlockReason.LOW_BATTERY, QueuePolicy.blockReason(it(1, st = DownloadStatus.RUNNING), low.copy(batteryPercent = 15)))
    }

    @Test fun restartLevelIsCappedSoFullBatteryCanRestart() {
        assertEquals(20, QueuePolicy.restartBelowOrAt(15))
        assertEquals(99, QueuePolicy.restartBelowOrAt(96))
        assertEquals(99, QueuePolicy.restartBelowOrAt(99))
        val high = ok.copy(lowBatteryThreshold = 98, charging = false, batteryPercent = 100)
        assertEquals(BlockReason.NONE, QueuePolicy.blockReason(it(1), high))
    }
}
