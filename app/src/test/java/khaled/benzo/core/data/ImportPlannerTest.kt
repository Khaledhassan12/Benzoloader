package khaled.benzo.core.data

import khaled.benzo.core.catalog.FolderNames
import khaled.benzo.data.model.FileCategory
import org.junit.Assert.*
import org.junit.Test

class ImportPlannerTest {
    private fun planner(catalog: Boolean = false, onDisk: Set<Pair<String, String>> = emptySet()) =
        ImportPlanner("/r", catalog, FolderNames()) { f, n -> (f to n) in onDisk }

    @Test fun knownUrlsAreSkipped() {
        val out = planner().plan(listOf(ImportEntry("https://a/x.zip", null), ImportEntry("https://a/y.zip", null)), setOf("https://a/x.zip"), emptySet())
        assertEquals(listOf("y.zip"), out.map { it.fileName })
    }

    @Test fun sameNameTwiceGetsNumberedInTheSameBatch() {
        val out = planner().plan(listOf(ImportEntry("https://a/f.zip", null), ImportEntry("https://b/f.zip", null)), emptySet(), emptySet())
        assertEquals(listOf("f.zip", "f (1).zip"), out.map { it.fileName })
    }

    @Test fun existingFileOnDiskForcesNewName() {
        val out = planner(onDisk = setOf("/r" to "f.zip")).plan(listOf(ImportEntry("https://a/f.zip", null)), emptySet(), emptySet())
        assertEquals("f (1).zip", out[0].fileName)
    }

    @Test fun catalogSendsImagesAndUnknownToTheirFolders() {
        val out = planner(catalog = true).plan(listOf(ImportEntry("https://a/p.jpg", null), ImportEntry("https://a/q.xyz", null)), emptySet(), emptySet())
        assertEquals(listOf("/r/Images", "/r/Other"), out.map { it.folder })
        assertEquals(listOf(FileCategory.IMAGE, FileCategory.OTHER), out.map { it.category })
    }
}
