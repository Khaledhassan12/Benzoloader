package khaled.benzo.core.download

import org.junit.Assert.assertEquals
import org.junit.Test

class SizeFormatTest {
    @Test fun allUnitsPickLargestReached() {
        assertEquals("1.50 MB", SizeFormat.format(1572864, 2, SizeFormat.ALL))
        assertEquals("512 B", SizeFormat.format(512, 2, SizeFormat.ALL))
        assertEquals("0 B", SizeFormat.format(0, 2, SizeFormat.ALL))
    }
    @Test fun skippedUnitFallsBackToTickedOne() {
        assertEquals("1536.00 KB", SizeFormat.format(1572864, 2, SizeFormat.B or SizeFormat.KB))
        assertEquals("0.00 GB", SizeFormat.format(10, 2, SizeFormat.GB).replace("0.00 GB", "0.00 GB"))
    }
    @Test fun emptyMaskMeansAll() = assertEquals(SizeFormat.ALL, SizeFormat.sanitize(0))
    @Test fun terabytesNeedGb() {
        assertEquals("2.00 TB", SizeFormat.format(2L * 1099511627776L, 2, SizeFormat.ALL))
        assertEquals("2097152.00 MB", SizeFormat.format(2L * 1099511627776L, 2, SizeFormat.MB))
    }
}
