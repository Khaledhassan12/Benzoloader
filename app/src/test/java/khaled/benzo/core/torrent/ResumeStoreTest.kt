package khaled.benzo.core.torrent

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ResumeStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun roundTrip() {
        val f = ResumeStore.fileFor(tmp.root, "ab12")
        assertTrue(ResumeStore.save(f, byteArrayOf(1, 2, 3)))
        assertArrayEquals(byteArrayOf(1, 2, 3), ResumeStore.load(f))
        assertFalse(java.io.File(tmp.root, "ab12.fastresume.tmp").exists())
    }
    @Test fun missingEmptyAndHugeMeanNoData() {
        val f = ResumeStore.fileFor(tmp.root, "x")
        assertNull(ResumeStore.load(f))
        assertFalse(ResumeStore.save(f, ByteArray(0)))
        assertFalse(ResumeStore.save(f, ByteArray(ResumeStore.MAX_BYTES + 1)))
        f.writeBytes(ByteArray(0)); assertNull(ResumeStore.load(f))
    }
    @Test fun overwriteAndDelete() {
        val f = ResumeStore.fileFor(tmp.root, "y")
        ResumeStore.save(f, byteArrayOf(1)); ResumeStore.save(f, byteArrayOf(9, 9))
        assertArrayEquals(byteArrayOf(9, 9), ResumeStore.load(f))
        ResumeStore.delete(f); assertNull(ResumeStore.load(f))
    }

    @Test fun deleteAllForRemovesOnlyThatTorrentsFiles() {
        listOf("ab.torrent", "ab.prio", "ab.fastresume", "cd.torrent").forEach { java.io.File(tmp.root, it).writeText("x") }
        ResumeStore.deleteAllFor(tmp.root, "ab")
        assertFalse(java.io.File(tmp.root, "ab.torrent").exists())
        assertFalse(java.io.File(tmp.root, "ab.prio").exists())
        assertFalse(java.io.File(tmp.root, "ab.fastresume").exists())
        assertTrue(java.io.File(tmp.root, "cd.torrent").exists())
    }
    @Test fun deleteAllForIgnoresUnsafeHash() {
        val f = java.io.File(tmp.root, "keep.torrent"); f.writeText("x")
        ResumeStore.deleteAllFor(tmp.root, "../keep")
        ResumeStore.deleteAllFor(tmp.root, "")
        assertTrue(f.exists())
    }
}
