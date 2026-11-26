package khaled.benzo.core.batch

import khaled.benzo.core.batch.BatchPolicy.Range
import khaled.benzo.core.batch.BatchPolicy.Result
import org.junit.Assert.*
import org.junit.Test

/** WRITTEN, NOT RUN. The $$$ = 001 rule is from the reference's Help text; the rest is my own reading, see BatchPolicy. */
class BatchPolicyTest {
    private fun links(t: String, r: Range) = (BatchPolicy.expand(t, r) as Result.Ok).links
    @Test fun dollarRunGivesLeadingZeros() =
        assertEquals(listOf("http://h.example/a001.bin", "http://h.example/a002.bin"), links("http://h.example/a\$\$\$.bin", Range.Numbers(1, 2, 1)))
    @Test fun singleDollarHasNoPadding() =
        assertEquals(listOf("http://h.example/9", "http://h.example/10"), links("http://h.example/\$", Range.Numbers(9, 10, 1)))
    @Test fun stepSkipsValues() = assertEquals(listOf("0", "3", "6", "9"), links("http://h.example/\$", Range.Numbers(0, 9, 3)).map { it.substringAfterLast('/') })
    @Test fun everyPlaceGetsTheSameValue() =
        assertEquals(listOf("http://h.example/1/f1", "http://h.example/2/f2"), links("http://h.example/\$/f\$", Range.Numbers(1, 2, 1)))
    @Test fun letters() = assertEquals(listOf("a", "b", "c"), links("http://h.example/\$", Range.Letters('a', 'c')).map { it.substringAfterLast('/') })
    @Test fun rejections() {
        assertEquals(Result.Error.NO_PLACE, BatchPolicy.expand("http://h.example/x", Range.Numbers(0, 1, 1)))
        assertEquals(Result.Error.NOT_WEB_LINK, BatchPolicy.expand("ftp://h/\$", Range.Numbers(0, 1, 1)))
        assertEquals(Result.Error.BAD_STEP, BatchPolicy.expand("http://h/\$", Range.Numbers(0, 1, 0)))
        assertEquals(Result.Error.EMPTY_RANGE, BatchPolicy.expand("http://h/\$", Range.Numbers(5, 1, 1)))
        assertEquals(Result.Error.BAD_LETTER, BatchPolicy.expand("http://h/\$", Range.Letters('a', 'Z')))
        assertEquals(Result.Error.BAD_LETTER, BatchPolicy.expand("http://h/\$", Range.Letters('1', '3')))
        assertEquals(Result.Error.TOO_MANY, BatchPolicy.expand("http://h/\$", Range.Numbers(0, 5000, 1)))
    }
    @Test fun exactlyTheCapIsAllowed() = assertEquals(1000, links("http://h/\$", Range.Numbers(0, 999, 1)).size)
}
