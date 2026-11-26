package khaled.benzo.core.download

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedFormatTest {
    private val gmk = SizeFormat.UPPER_UNITS or SizeFormat.UPPER_B
    @Test fun defaultIsCurrentBytes() = assertEquals("1.00MB/s", SpeedFormat.text(1048576, null, SpeedFormat.DEFAULT, gmk))
    @Test fun bitsMultiplyByEight() = assertEquals("8.00MB/s", SpeedFormat.text(1048576, null, SpeedFormat.CURRENT or SpeedFormat.BIT, gmk))
    @Test fun averageShownAlone() = assertEquals("1.00KB/s", SpeedFormat.text(2048, 1024, SpeedFormat.AVERAGE or SpeedFormat.BYTE, gmk))
    @Test fun averageUnknownFallsBackToCurrent() = assertEquals("2.00KB/s", SpeedFormat.text(2048, null, SpeedFormat.AVERAGE or SpeedFormat.BYTE, gmk))
    @Test fun pairsExclusive() {
        assertEquals(SpeedFormat.DEFAULT, SpeedFormat.sanitize(0))
        assertEquals(SpeedFormat.CURRENT or SpeedFormat.BYTE, SpeedFormat.sanitize(15))
    }
}
