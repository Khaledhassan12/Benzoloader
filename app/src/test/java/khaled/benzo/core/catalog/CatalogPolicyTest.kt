package khaled.benzo.core.catalog

import khaled.benzo.data.model.FileCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogPolicyTest {
    private val root = "/storage/emulated/0/Download"

    @Test fun sortsIntoTypeFolder() =
        assertEquals("$root/Video", CatalogPolicy.targetFolder(root, root, FileCategory.VIDEO, true))

    @Test fun trailingSlashStillMatchesRoot() =
        assertEquals("$root/Archives", CatalogPolicy.targetFolder("$root/", root, FileCategory.ARCHIVE, true))

    @Test fun disabledKeepsPath() =
        assertEquals(root, CatalogPolicy.targetFolder(root, root, FileCategory.VIDEO, false))

    @Test fun customFolderIsRespected() =
        assertEquals("/sdcard/Mine", CatalogPolicy.targetFolder("/sdcard/Mine", root, FileCategory.VIDEO, true))

    @Test fun categoriesWithoutFolderKeepPath() =
        assertEquals(root, CatalogPolicy.targetFolder(root, root, FileCategory.ALL, true))

    @Test fun fileNameCategoriesResolveFromFullName() {
        assertEquals(FileCategory.VIDEO, FileCategory.fromExtension("clip.MP4"))
        assertEquals(FileCategory.ARCHIVE, FileCategory.fromExtension(".zip"))
        assertEquals(FileCategory.PROGRAM, FileCategory.fromExtension("app.apk"))
        assertEquals(FileCategory.OTHER, FileCategory.fromExtension("noextension"))
        assertEquals(FileCategory.OTHER, FileCategory.fromExtension("data.xyz"))
        assertEquals(FileCategory.IMAGE, FileCategory.fromExtension("photo.JPG"))
        assertEquals(FileCategory.DOCUMENT, FileCategory.fromExtension("a.pdf"))
    }
}
