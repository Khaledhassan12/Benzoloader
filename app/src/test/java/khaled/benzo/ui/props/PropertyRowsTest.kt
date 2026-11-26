package khaled.benzo.ui.props

import khaled.benzo.data.model.DownloadItem
import org.junit.Assert.*
import org.junit.Test

/** WRITTEN, NOT RUN. */
class PropertyRowsTest {
    private val item = DownloadItem(url = "u", fileName = "f", savePath = "/p", totalBytes = 10, downloadedBytes = 4)
    @Test fun onlyRealFieldsAppear() {
        val keys = PropertyRows.build(item, { "$it B" }, { "t" }).map { it.key }
        assertTrue("name" in keys); assertFalse("hash" in keys); assertFalse("checksum" in keys); assertFalse("finished" in keys)
    }
    @Test fun hiddenKeysAreFilteredButNotRemovedFromBuild() {
        val rows = PropertyRows.build(item, { "$it B" }, { "t" })
        assertFalse(PropertyRows.visible(rows, setOf("url")).any { it.key == "url" })
        assertTrue(rows.any { it.key == "url" })
    }
}
