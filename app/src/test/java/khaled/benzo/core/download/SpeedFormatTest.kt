package khaled.benzo.core.download

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedFormatTest {
    @Test fun defaultIsCurrentBytes() = assertEquals("1.0 MB/s", SpeedFormat.text(1048576, null, SpeedFormat.DEFAULT))
    @Test fun bitsOnly() = assertEquals("8.0 Mb/s", SpeedFormat.text(1000000, null, SpeedFormat.CURRENT or SpeedFormat.BIT))
    @Test fun currentAndAverage() = assertEquals("2.0 KB/s (1.0 KB/s)", SpeedFormat.text(2048, 1024, SpeedFormat.CURRENT or SpeedFormat.AVERAGE or SpeedFormat.BYTE))
    @Test fun averageUnknownFallsBackToCurrent() = assertEquals("2.0 KB/s", SpeedFormat.text(2048, null, SpeedFormat.AVERAGE or SpeedFormat.BYTE))
    @Test fun emptyChoicesGetDefaults() = assertEquals(SpeedFormat.DEFAULT, SpeedFormat.sanitize(0))
}
