package khaled.benzo.core.queue

import khaled.benzo.data.model.DownloadItem
import khaled.benzo.data.model.DownloadStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class QueuePolicyTest {
    private fun it(id: Long, name: String = "f$id", st: DownloadStatus = DownloadStatus.QUEUED, wifi: Boolean = false, size: Long = id * 10, created: Long = id) =
        DownloadItem(id = id, url = "http://x/$name", fileName = name, savePath = "/tmp", status = st, isWifiOnly = wifi, totalBytes = size, createdDate = created)

    private val ok = Conditions(true, true, 80, false, false, 15, 1_000, null, 0, 2)

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
    @Test fun searchIsCaseInsensitive() = assertEquals(listOf(2L), ListLogic.apply(items, StatusFilter.ALL, "a.MP", SortKey.TIME, true).map { it.id })
    @Test fun sortsByNameSizeAndDirection() {
        assertEquals(listOf(2L, 1L, 3L, 4L, 5L), ListLogic.apply(items, StatusFilter.ALL, "", SortKey.NAME, true).map { it.id })
        assertEquals(listOf(2L, 5L, 4L, 1L, 3L), ListLogic.apply(items, StatusFilter.ALL, "", SortKey.SIZE, false).map { it.id })
    }
}
