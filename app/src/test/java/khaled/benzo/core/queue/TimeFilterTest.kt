package khaled.benzo.core.queue

import khaled.benzo.data.model.DownloadItem
import khaled.benzo.data.model.DownloadStatus
import khaled.benzo.data.model.FileCategory
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

// Written, not run.
class TimeFilterTest {
    private val zone = ZoneId.of("UTC")
    private val now = ZonedDateTime.of(2026, 11, 25, 15, 0, 0, 0, zone).toInstant().toEpochMilli()
    private fun daysAgo(d: Long) = ZonedDateTime.of(2026, 11, 25, 9, 0, 0, 0, zone).minusDays(d).toInstant().toEpochMilli()
    private fun item(id: Long, done: Long, created: Long = done) = DownloadItem(
        id = id, url = "https://x/$id", fileName = "f$id", savePath = "/x", status = DownloadStatus.COMPLETED,
        category = FileCategory.DOCUMENT, createdDate = created, completedDate = done)
    private val items = listOf(item(1, daysAgo(0)), item(2, daysAgo(1)), item(3, daysAgo(3)), item(4, daysAgo(20)), item(5, daysAgo(200)), item(6, daysAgo(500)))

    @Test fun windowsNest() {
        val c = TimeFilter.counts(items, now, zone)
        assertEquals(6, c[TimeFilter.ALL])
        assertEquals(1, c[TimeFilter.TODAY])
        assertEquals(1, c[TimeFilter.YESTERDAY])
        assertEquals(3, c[TimeFilter.WEEK])
        assertEquals(4, c[TimeFilter.MONTH])
        assertEquals(5, c[TimeFilter.YEAR])
    }

    @Test fun zeroCountsAreOmitted() {
        val c = TimeFilter.counts(listOf(item(1, daysAgo(400))), now, zone)
        assertFalse(c.containsKey(TimeFilter.TODAY))
        assertFalse(c.containsKey(TimeFilter.YEAR))
        assertEquals(1, c[TimeFilter.ALL])
    }

    @Test fun finishTimeWinsOverAddTime() {
        val finishedToday = item(7, done = daysAgo(0), created = daysAgo(40))
        assertTrue(TimeFilter.TODAY.matches(finishedToday, now, zone))
        val notDone = finishedToday.copy(completedDate = 0L, status = DownloadStatus.PAUSED)
        assertFalse(TimeFilter.TODAY.matches(notDone, now, zone))
        assertTrue(TimeFilter.MONTH.matches(notDone.copy(createdDate = daysAgo(10)), now, zone))
    }

    @Test fun listStateAppliesTimeAndOrderSort() {
        val st = ListState().withTime(TimeFilter.WEEK).withSort(SortKey.ORDER)
        val out = st.apply(items, now).map { it.id }
        assertEquals(setOf(1L, 2L, 3L), out.toSet())
        assertEquals(listOf(3L, 2L, 1L), out)
    }
}
