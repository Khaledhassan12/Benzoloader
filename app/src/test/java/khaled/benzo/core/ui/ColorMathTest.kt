package khaled.benzo.core.ui

import org.junit.Assert.*
import org.junit.Test

/** WRITTEN, NOT RUN. */
class ColorMathTest {
    @Test fun formatIsEightUpperDigits() {
        assertEquals("FF000000", ColorMath.format(0xFF000000.toInt()))
        assertEquals("26FFAA00", ColorMath.format(0x26FFAA00.toInt()))
    }
    @Test fun parseAcceptsEightOrSixDigits() {
        assertEquals(0xFF112233.toInt(), ColorMath.parse("112233"))
        assertEquals(0x80112233.toInt(), ColorMath.parse("#80112233"))
        assertEquals(0xFFAABBCC.toInt(), ColorMath.parse(" aabbcc "))
    }
    @Test fun parseRejectsBadText() {
        assertNull(ColorMath.parse("")); assertNull(ColorMath.parse("12345")); assertNull(ColorMath.parse("GG0000")); assertNull(ColorMath.parse("123456789"))
    }
    @Test fun primaries() {
        assertEquals(0xFFFF0000.toInt(), ColorMath.toArgb(255, 0f, 1f, 1f))
        assertEquals(0xFF00FF00.toInt(), ColorMath.toArgb(255, 120f, 1f, 1f))
        assertEquals(0xFF0000FF.toInt(), ColorMath.toArgb(255, 240f, 1f, 1f))
        assertEquals(0xFF000000.toInt(), ColorMath.toArgb(255, 200f, 0.7f, 0f))
        assertEquals(0x80FFFFFF.toInt(), ColorMath.toArgb(128, 10f, 0f, 1f))
    }
    @Test fun roundTripKeepsRgb() {
        for (c in listOf(0xFF336699.toInt(), 0xFFCC8800.toInt(), 0xFF808080.toInt(), 0xFFFFFFFF.toInt())) {
            val h = ColorMath.toHsv(c)
            val back = ColorMath.toArgb(255, h.h, h.s, h.v)
            for (shift in listOf(16, 8, 0)) assertTrue(kotlin.math.abs(((c shr shift) and 0xFF) - ((back shr shift) and 0xFF)) <= 1)
        }
    }
    @Test fun alphaExtraction() = assertEquals(0x26, ColorMath.alphaOf(0x26112233))
}
