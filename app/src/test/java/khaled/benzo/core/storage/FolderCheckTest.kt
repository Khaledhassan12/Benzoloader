package khaled.benzo.core.storage

import khaled.benzo.core.storage.FolderCheck.Verdict
import org.junit.Assert.*
import org.junit.Test

class FolderCheckTest {
    private class Fake(val dirs: Set<String>, val files: Set<String> = emptySet(), val writable: Set<String> = emptySet()) : FolderFs {
        override fun exists(path: String) = path in dirs || path in files
        override fun isDirectory(path: String) = path in dirs
        override fun canCreateFileIn(dir: String) = dir in writable
    }

    private val fs = Fake(
        dirs = setOf("/", "/storage", "/storage/emulated", "/storage/emulated/0", "/storage/emulated/0/Download", "/storage/emulated/0/Locked"),
        files = setOf("/storage/emulated/0/readme.txt"),
        writable = setOf("/storage/emulated/0/Download", "/storage/emulated/0")
    )

    @Test fun existingWritableFolderIsOk() = assertEquals(Verdict.OK, FolderCheck.check("/storage/emulated/0/Download", fs))

    @Test fun trailingSlashIsIgnored() = assertEquals(Verdict.OK, FolderCheck.check("/storage/emulated/0/Download/", fs))

    @Test fun missingFolderUnderWritableParentIsOk() = assertEquals(Verdict.OK, FolderCheck.check("/storage/emulated/0/Download/New/Deep", fs))

    @Test fun missingFolderUnderLockedParentIsNotWritable() = assertEquals(Verdict.NOT_WRITABLE, FolderCheck.check("/storage/emulated/0/Locked/x", fs))

    @Test fun existingButNotWritable() = assertEquals(Verdict.NOT_WRITABLE, FolderCheck.check("/storage/emulated/0/Locked", fs))

    @Test fun aFileIsNotAFolder() {
        assertEquals(Verdict.NOT_A_FOLDER, FolderCheck.check("/storage/emulated/0/readme.txt", fs))
        assertEquals(Verdict.NOT_A_FOLDER, FolderCheck.check("/storage/emulated/0/readme.txt/sub", fs))
    }

    @Test fun emptyAndRelativePaths() {
        assertEquals(Verdict.EMPTY, FolderCheck.check("   ", fs))
        assertEquals(Verdict.NOT_ABSOLUTE, FolderCheck.check("Download", fs))
    }

    @Test fun rootThatDoesNotExistIsNotWritable() = assertEquals(Verdict.NOT_WRITABLE, FolderCheck.check("/nope/x", Fake(emptySet())))

    @Test fun parentOf() {
        assertEquals("/a", FolderCheck.parentOf("/a/b")); assertEquals("/", FolderCheck.parentOf("/a")); assertNull(FolderCheck.parentOf("/"))
    }
}
