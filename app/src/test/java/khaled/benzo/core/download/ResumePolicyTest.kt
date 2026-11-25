package khaled.benzo.core.download

import org.junit.Assert.*
import org.junit.Test

class ResumePolicyTest {
    private fun v(e: String?, m: String?, l: Long) = Validator(e, m, l)

    @Test fun sameEtagResumes() = assertTrue(ResumePolicy.canResume(v("\"a\"", null, 10), v("\"a\"", null, 10)))
    @Test fun differentEtagRestarts() = assertFalse(ResumePolicy.canResume(v("\"a\"", null, 10), v("\"b\"", null, 10)))
    @Test fun differentLengthRestarts() = assertFalse(ResumePolicy.canResume(v("\"a\"", null, 10), v("\"a\"", null, 11)))
    @Test fun weakEtagIgnoredButLastModifiedUsed() {
        assertFalse(ResumePolicy.canResume(v("W/\"a\"", "Mon", 10), v("W/\"b\"", "Tue", 10)))
        assertTrue(ResumePolicy.canResume(v("W/\"a\"", "Mon", 10), v("W/\"b\"", "Mon", 10)))
    }
    @Test fun nothingToCompareResumes() {
        assertTrue(ResumePolicy.canResume(null, v("x", null, 1)))
        assertTrue(ResumePolicy.canResume(v(null, null, 10), v("\"b\"", "Tue", 10)))
        assertTrue(ResumePolicy.canResume(v("\"a\"", null, 10), null))
    }
    @Test fun serializeRoundTrip() {
        val x = v("\"a\"", "Mon, 1 Jan", 123)
        assertEquals(x, Validator.parse(x.serialize()))
        assertEquals(v(null, null, 5), Validator.parse(v("W/\"w\"", null, 5).serialize()))
        assertNull(Validator.parse("junk"))
    }
    @Test fun partsRespectMinChunkAndRange() {
        assertEquals(1, ChunkPlanner.partsFor(100_000, 8, true))
        assertEquals(2, ChunkPlanner.partsFor(1_100_000, 8, true))
        assertEquals(8, ChunkPlanner.partsFor(100_000_000, 8, true))
        assertEquals(1, ChunkPlanner.partsFor(100_000_000, 8, false))
        assertEquals(1, ChunkPlanner.partsFor(-1, 8, true))
        assertEquals(16, ChunkPlanner.partsFor(1_000_000_000, 99, true))
    }
    @Test fun rangesCoverExactly() {
        for (total in listOf(1L, 7L, 1000L, 1_048_577L)) for (parts in 1..6) {
            val r = ChunkPlanner.ranges(total, minOf(parts, total.toInt().coerceAtLeast(1)))
            assertEquals(0L, r.first().first)
            assertEquals(total - 1, r.last().last)
            for (i in 1 until r.size) assertEquals(r[i - 1].last + 1, r[i].first)
        }
    }
}
