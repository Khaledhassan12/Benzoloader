package khaled.benzo.core.files

import khaled.benzo.data.model.DownloadStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class FileNamingTest {
    @Test fun freeNameIsKept() = assertEquals("a.zip", FileNaming.uniqueName("a.zip") { false })

    @Test fun numbersBeforeExtension() =
        assertEquals("a (2).zip", FileNaming.uniqueName("a.zip") { it == "a.zip" || it == "a (1).zip" })

    @Test fun noExtension() = assertEquals("README (1)", FileNaming.uniqueName("README") { it == "README" })

    @Test fun dotFileIsNotAnExtension() = assertEquals(".bashrc (1)", FileNaming.uniqueName(".bashrc") { it == ".bashrc" })

    @Test fun tarGzKeepsLastExtension() =
        assertEquals("x.tar (1).gz", FileNaming.uniqueName("x.tar.gz") { it == "x.tar.gz" })

    @Test fun errorPolicy() {
        assertEquals(DownloadStatus.PAUSED, ErrorPolicy.statusAfterFailure(true))
        assertEquals(DownloadStatus.ERROR, ErrorPolicy.statusAfterFailure(false))
    }
}
