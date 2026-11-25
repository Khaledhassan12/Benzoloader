package khaled.benzo.core.queue

import khaled.benzo.data.model.DownloadItem
import khaled.benzo.data.model.DownloadStatus
import khaled.benzo.data.model.FileCategory
import org.junit.Assert.*
import org.junit.Test

class ListStateTest {
    private fun d(id: Long, name: String, st: DownloadStatus, cat: FileCategory, size: Long = 0, t: Long = id) =
        DownloadItem(id = id, url = "https://x/$name", fileName = name, savePath = "/x", status = st, category = cat, totalBytes = size, createdDate = t)

    private val items = listOf(
        d(1, "b.mp4", DownloadStatus.RUNNING, FileCategory.VIDEO, 300),
        d(2, "a.zip", DownloadStatus.PAUSED, FileCategory.ARCHIVE, 100),
        d(3, "c.mp3", DownloadStatus.ERROR, FileCategory.MUSIC, 200),
        d(4, "d.jpg", DownloadStatus.COMPLETED, FileCategory.IMAGE, 50)
    )

    @Test fun drawerStatusEntriesSetStatusAndClearType() {
        val active = ListState(type = FileCategory.VIDEO).withCategory(FileCategory.DOWNLOADING)
        assertEquals(StatusFilter.ACTIVE, active.status)
        assertNull(active.type)
        assertEquals(StatusFilter.COMPLETED, ListState().withCategory(FileCategory.COMPLETED).status)
        assertEquals(StatusFilter.QUEUED, ListState().withCategory(FileCategory.QUEUED).status)
    }

    @Test fun typeEntryFiltersByTypeAndResetsStatus() {
        val s = ListState(status = StatusFilter.FAILED).withCategory(FileCategory.IMAGE)
        assertEquals(StatusFilter.ALL, s.status)
        assertEquals(listOf("d.jpg"), s.apply(items).map { it.fileName })
    }

    @Test fun failedAndPausedFilters() {
        assertEquals(listOf("c.mp3"), ListState().withStatus(StatusFilter.FAILED).apply(items).map { it.fileName })
        assertEquals(listOf("a.zip"), ListState().withStatus(StatusFilter.PAUSED).apply(items).map { it.fileName })
    }

    @Test fun searchIsCaseInsensitiveOnName() {
        assertEquals(listOf("b.mp4"), ListState().withQuery(" B.MP").apply(items).map { it.fileName })
    }

    @Test fun repeatedSortPickFlipsDirectionNewKeyResets() {
        val byName = ListState().withSort(SortKey.NAME)
        assertEquals(listOf("a.zip", "b.mp4", "c.mp3", "d.jpg"), byName.apply(items).map { it.fileName })
        val flipped = byName.withSort(SortKey.NAME)
        assertEquals(listOf("d.jpg", "c.mp3", "b.mp4", "a.zip"), flipped.apply(items).map { it.fileName })
        val bySize = flipped.withSort(SortKey.SIZE)
        assertFalse(bySize.ascending)
        assertEquals(listOf(300L, 200L, 100L, 50L), bySize.apply(items).map { it.totalBytes })
    }

    // Written, not run.
    @Test fun timeSortFollowsAddToBottomChoice() {
        val top = ListState().apply(items).map { it.id }
        assertEquals(listOf(4L, 3L, 2L, 1L), top)
        val bottom = ListState().withAddToBottom(true)
        assertEquals(listOf(1L, 2L, 3L, 4L), bottom.apply(items).map { it.id })
        // Leaving the time sort and coming back keeps the stored choice.
        val back = bottom.withSort(SortKey.NAME).withSort(SortKey.TIME)
        assertTrue(back.ascending)
        assertEquals(listOf(1L, 2L, 3L, 4L), back.apply(items).map { it.id })
        // Switching the setting back restores newest on top.
        assertEquals(listOf(4L, 3L, 2L, 1L), back.withAddToBottom(false).apply(items).map { it.id })
    }

    @Test fun addToBottomDoesNotTouchOtherSorts() {
        val byName = ListState().withSort(SortKey.NAME)
        assertTrue(byName.withAddToBottom(true).ascending)
        val bySize = ListState().withSort(SortKey.SIZE)
        assertFalse(bySize.withAddToBottom(true).ascending)
    }
}
