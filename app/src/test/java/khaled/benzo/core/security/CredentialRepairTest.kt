package khaled.benzo.core.security

import khaled.benzo.data.model.DownloadItem
import khaled.benzo.data.model.DownloadStatus
import org.junit.Assert.*
import org.junit.Test

class CredentialRepairTest {
    private val broken = DownloadItem(
        id = 7, url = "https://a/b", fileName = "b", savePath = "/x", downloadedBytes = 500, totalBytes = 1000,
        status = DownloadStatus.ERROR, errorMessage = "x", rawCookies = "enc1:zzz", rawHeaders = "enc1:yyy"
    )

    @Test fun newValuesReplaceAndProgressIsKept() {
        val r = CredentialRepair.apply(broken, " sid=1 ", "{\"A\":\"B\"}", false)
        assertEquals("sid=1", r.cookies); assertNull(r.rawCookies); assertNull(r.rawHeaders)
        assertEquals(500, r.downloadedBytes); assertEquals(1000, r.totalBytes)
        assertEquals(DownloadStatus.PAUSED, r.status); assertNull(r.errorMessage)
    }

    @Test fun partialEntryKeepsTheOtherStoredValueAndStaysInError() {
        val r = CredentialRepair.apply(broken, "sid=1", "", false)
        assertNull(r.rawCookies); assertEquals("enc1:yyy", r.rawHeaders)
        assertEquals(DownloadStatus.ERROR, r.status)
    }

    @Test fun blankWithoutDiscardChangesNothing() {
        assertEquals(broken, CredentialRepair.apply(broken, "", null, false))
    }

    @Test fun discardDropsOnlyUnreadableValues() {
        val r = CredentialRepair.apply(broken, null, null, true)
        assertFalse(r.needsReentry); assertNull(r.cookies); assertEquals(DownloadStatus.PAUSED, r.status)
    }

    @Test fun healthyItemIsUntouched() {
        val ok = broken.copy(rawCookies = null, rawHeaders = null)
        assertEquals(ok, CredentialRepair.apply(ok, "z", "z", true))
    }
}
