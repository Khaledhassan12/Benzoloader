package khaled.benzo.core.catalog

import khaled.benzo.data.model.FileCategory
import org.junit.Assert.*
import org.junit.Test

class FolderNamesTest {
    private val root = "/r"

    @Test fun defaultsIncludeImagesAndOther() {
        assertEquals("/r/Images", CatalogPolicy.targetFolder(root, root, FileCategory.IMAGE, true))
        assertEquals("/r/Other", CatalogPolicy.targetFolder(root, root, FileCategory.OTHER, true))
    }

    @Test fun customNameIsUsed() {
        val n = FolderNames(mapOf(FileCategory.VIDEO to "Clips"))
        assertEquals("/r/Clips", CatalogPolicy.targetFolder(root, root, FileCategory.VIDEO, true, n))
        assertEquals("/r/Music", CatalogPolicy.targetFolder(root, root, FileCategory.MUSIC, true, n))
    }

    @Test fun unsafeNamesFallBackToDefault() {
        for (bad in listOf("", "  ", ".", "..", "a/b", "..\\x", "../up")) {
            val n = FolderNames.fromInput(mapOf(FileCategory.VIDEO to bad))
            assertEquals("input '$bad'", "Video", n.nameFor(FileCategory.VIDEO))
        }
    }

    @Test fun encodeDecodeRoundTripAndDefaultsAreNotStored() {
        val n = FolderNames.fromInput(mapOf(FileCategory.VIDEO to "Clips", FileCategory.MUSIC to "Music", FileCategory.IMAGE to " Pics "))
        val back = FolderNames.decode(n.encode())
        assertEquals("Clips", back.nameFor(FileCategory.VIDEO))
        assertEquals("Pics", back.nameFor(FileCategory.IMAGE))
        assertFalse(n.encode().contains("music="))
    }

    @Test fun decodeIgnoresGarbageAndNonEditableCategories() {
        val back = FolderNames.decode("video=Ok\nnonsense\nall=X\n=y\nmusic=../bad")
        assertEquals("Ok", back.nameFor(FileCategory.VIDEO))
        assertEquals("Music", back.nameFor(FileCategory.MUSIC))
        assertNull(back.nameFor(FileCategory.ALL))
    }
}
