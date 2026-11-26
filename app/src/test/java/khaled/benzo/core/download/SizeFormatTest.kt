package khaled.benzo.core.download

import org.junit.Assert.assertEquals
import org.junit.Test

class SizeFormatTest {
    private val gmk = SizeFormat.UPPER_UNITS or SizeFormat.UPPER_B
    @Test fun compactSteps() {
        assertEquals("0K", SizeFormat.compact(0, gmk))
        assertEquals("0.50K", SizeFormat.compact(512, gmk))
        assertEquals("1.50M", SizeFormat.compact(1572864, gmk))
        assertEquals("12.0M", SizeFormat.compact(12L * 1048576, gmk))
        assertEquals("150M", SizeFormat.compact(150L * 1048576, gmk))
        assertEquals("1.25G", SizeFormat.compact(1280L * 1048576, gmk))
        assertEquals("2.00T", SizeFormat.compact(2L * 1099511627776L, gmk))
    }
    @Test fun lowercaseLetters() = assertEquals("1.50m", SizeFormat.compact(1572864, SizeFormat.LOWER_UNITS or SizeFormat.UPPER_B))
    @Test fun formatAddsByteLetter() {
        assertEquals("1.50MB", SizeFormat.format(1572864, 2, gmk))
        assertEquals("1.50mb", SizeFormat.format(1572864, 2, SizeFormat.LOWER_UNITS or SizeFormat.LOWER_B))
    }
    @Test fun pairsAreExclusiveAndNeverEmpty() {
        assertEquals(SizeFormat.DEFAULT, SizeFormat.sanitize(0))
        assertEquals(SizeFormat.DEFAULT, SizeFormat.sanitize(15))
        assertEquals(SizeFormat.UPPER_UNITS or SizeFormat.LOWER_B, SizeFormat.sanitize(SizeFormat.UPPER_UNITS or SizeFormat.LOWER_B))
    }
}
