package khaled.benzo.core.queue

import khaled.benzo.data.model.DownloadItem
import khaled.benzo.data.model.DownloadStatus
import khaled.benzo.data.model.FileCategory
import org.junit.Assert.*
import org.junit.Test

// Written, not run.
class QuickAddPolicyTest {
    @Test fun parentOffNeverSkipsOrStarts() {
        assertEquals(QuickAddDecision(false, false), QuickAddPolicy.decide(false, true, true))
        assertEquals(QuickAddDecision(false, false), QuickAddPolicy.decide(false, false, true))
    }

    @Test fun parentOnChildOffSkipsButDoesNotStart() {
        assertEquals(QuickAddDecision(true, false), QuickAddPolicy.decide(true, false, true))
    }

    @Test fun bothOnSkipsAndStarts() {
        assertEquals(QuickAddDecision(true, true), QuickAddPolicy.decide(true, true, true))
    }

    @Test fun nonExternalSourcesAlwaysShowTheEditor() {
        assertEquals(QuickAddDecision(false, false), QuickAddPolicy.decide(true, true, false))
    }

    @Test fun addedWithoutStartIsPausedAndTheSchedulerIgnoresIt() {
        val status = QuickAddPolicy.initialStatus(startNow = false)
        assertEquals(DownloadStatus.PAUSED, status)
        val item = DownloadItem(id = 1, url = "https://x/a.zip", fileName = "a.zip", savePath = "/x", status = status, category = FileCategory.ARCHIVE)
        val ok = Conditions(networkAvailable = true, onWifi = true, batteryPercent = 100, charging = true, globalWifiOnly = false,
            lowBatteryThreshold = 0, nowMillis = 0L, scheduleStartMillis = null, activeCount = 0, maxConcurrent = 5)
        assertTrue(QueuePolicy.nextToStart(listOf(item), ok).isEmpty())
        assertTrue(QueuePolicy.nextToStart(listOf(item), ok, newestFirst = true).isEmpty())
    }

    @Test fun addedWithStartIsQueuedAndStartsWhenRulesAllow() {
        val status = QuickAddPolicy.initialStatus(startNow = true)
        assertEquals(DownloadStatus.QUEUED, status)
        val item = DownloadItem(id = 1, url = "https://x/a.zip", fileName = "a.zip", savePath = "/x", status = status, category = FileCategory.ARCHIVE)
        val ok = Conditions(networkAvailable = true, onWifi = true, batteryPercent = 100, charging = true, globalWifiOnly = false,
            lowBatteryThreshold = 0, nowMillis = 0L, scheduleStartMillis = null, activeCount = 0, maxConcurrent = 5)
        assertEquals(listOf(1L), QueuePolicy.nextToStart(listOf(item), ok).map { it.id })
    }
}
