package khaled.benzo.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadChunkTest {
    private fun chunk(cur: Long) = DownloadChunk(downloadId = 1, partIndex = 0, startByte = 100, currentByte = cur, endByte = 999)

    @Test fun untrackedUsesLivePosition() = assertEquals(500L, chunk(500).persistedByte())
    @Test fun flushedPositionIsSavedWhileLiveIsAhead() {
        val c = chunk(500); c.durableByte = 300
        assertEquals(300L, c.persistedByte())
    }
    @Test fun flushedPositionBehindStartOrAheadOfLiveIsIgnored() {
        val c = chunk(500); c.durableByte = 50
        assertEquals(500L, c.persistedByte())
        c.durableByte = 700
        assertEquals(500L, c.persistedByte())
    }
    @Test fun copyDoesNotCarryTheFlushedPosition() {
        val c = chunk(500); c.durableByte = 300
        assertEquals(-1L, c.copy().durableByte)
    }
}
