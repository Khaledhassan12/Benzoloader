package khaled.benzo.core.queue

import khaled.benzo.data.model.DownloadItem
import khaled.benzo.data.model.DownloadStatus
import org.junit.Assert.*
import org.junit.Test

/** WRITTEN, NOT RUN. */
class EditPolicyTest {
    private fun item(done: Long, st: DownloadStatus, id: Long = 1, name: String = "f") =
        DownloadItem(id = id, url = "u", fileName = name, savePath = "/p", downloadedBytes = done, status = st)
    @Test fun identityEditAllowedBeforeAnyData() =
        assertEquals(EditPolicy.Verdict.OK, EditPolicy.check(item(0, DownloadStatus.PAUSED), "u2", "f", "/p"))
    @Test fun identityEditBlockedAfterProgress() =
        assertEquals(EditPolicy.Verdict.LOCKED_BY_PROGRESS, EditPolicy.check(item(5, DownloadStatus.PAUSED), "u", "g", "/p"))
    @Test fun identityEditBlockedWhileRunningOrConnecting() {
        assertEquals(EditPolicy.Verdict.LOCKED_BY_PROGRESS, EditPolicy.check(item(0, DownloadStatus.RUNNING), "u", "f", "/q"))
        assertEquals(EditPolicy.Verdict.LOCKED_BY_PROGRESS, EditPolicy.check(item(0, DownloadStatus.CONNECTING), "u", "f", "/q"))
    }
    @Test fun completedItemIsLockedEvenWithZeroBytesRecorded() =
        assertEquals(EditPolicy.Verdict.LOCKED_BY_PROGRESS, EditPolicy.check(item(0, DownloadStatus.COMPLETED), "u", "g", "/p"))
    @Test fun existingFileAtOldLocationLocks() =
        assertEquals(EditPolicy.Verdict.LOCKED_BY_PROGRESS, EditPolicy.check(item(0, DownloadStatus.PAUSED), "u", "g", "/p", oldFileExists = true))
    @Test fun safeFieldsAlwaysAllowed() =
        assertEquals(EditPolicy.Verdict.OK, EditPolicy.check(item(5, DownloadStatus.COMPLETED), "u", "f", "/p"))
    @Test fun renameOntoAnotherItemIsRejected() =
        assertEquals(EditPolicy.Verdict.COLLISION, EditPolicy.check(item(0, DownloadStatus.PAUSED), "u", "g", "/p", others = listOf(item(0, DownloadStatus.QUEUED, id = 2, name = "g"))))
    @Test fun renameOntoExistingFileIsRejected() =
        assertEquals(EditPolicy.Verdict.COLLISION, EditPolicy.check(item(0, DownloadStatus.PAUSED), "u", "g", "/p", newFileExists = true))
    @Test fun ownRowIsNotACollision() =
        assertEquals(EditPolicy.Verdict.OK, EditPolicy.check(item(0, DownloadStatus.PAUSED), "u", "g", "/p", others = listOf(item(0, DownloadStatus.PAUSED, id = 1, name = "g"))))
    @Test fun runningAppliesNextStart() { assertTrue(EditPolicy.appliesNextStart(item(0, DownloadStatus.RUNNING))); assertFalse(EditPolicy.appliesNextStart(item(0, DownloadStatus.PAUSED))) }
}
