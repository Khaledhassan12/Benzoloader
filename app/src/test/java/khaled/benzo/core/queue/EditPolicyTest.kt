package khaled.benzo.core.queue

import khaled.benzo.data.model.DownloadItem
import khaled.benzo.data.model.DownloadStatus
import org.junit.Assert.*
import org.junit.Test

/** WRITTEN, NOT RUN. */
class EditPolicyTest {
    private fun item(done: Long, st: DownloadStatus) = DownloadItem(url = "u", fileName = "f", savePath = "/p", downloadedBytes = done, status = st)
    @Test fun identityEditAllowedBeforeAnyData() =
        assertEquals(EditPolicy.Verdict.OK, EditPolicy.check(item(0, DownloadStatus.PAUSED), "u2", "f", "/p"))
    @Test fun identityEditBlockedAfterProgress() =
        assertEquals(EditPolicy.Verdict.LOCKED_BY_PROGRESS, EditPolicy.check(item(5, DownloadStatus.PAUSED), "u", "g", "/p"))
    @Test fun identityEditBlockedWhileRunning() =
        assertEquals(EditPolicy.Verdict.LOCKED_BY_PROGRESS, EditPolicy.check(item(0, DownloadStatus.RUNNING), "u", "f", "/q"))
    @Test fun safeFieldsAlwaysAllowed() =
        assertEquals(EditPolicy.Verdict.OK, EditPolicy.check(item(5, DownloadStatus.RUNNING), "u", "f", "/p"))
    @Test fun runningAppliesNextStart() { assertTrue(EditPolicy.appliesNextStart(item(0, DownloadStatus.RUNNING))); assertFalse(EditPolicy.appliesNextStart(item(0, DownloadStatus.PAUSED))) }
}
