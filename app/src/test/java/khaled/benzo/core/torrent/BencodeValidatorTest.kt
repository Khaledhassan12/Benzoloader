package khaled.benzo.core.torrent

import org.junit.Assert.*
import org.junit.Test

class BencodeValidatorTest {
    private fun v(s: String) = BencodeValidator.validate(s.toByteArray(Charsets.ISO_8859_1))
    private fun bad(s: String) = assertTrue("expected Bad for: $s", v(s) is BencodeValidator.Result.Bad)

    @Test fun wellFormedTorrentShapeIsAccepted() {
        assertEquals(BencodeValidator.Result.Ok, v("d8:announce3:url4:infod6:lengthi5e4:name1:a12:piece lengthi16384ee"))
        assertEquals(BencodeValidator.Result.Ok, v("d1:ald1:bi-3eee1:cle"))
    }

    @Test fun notADictionaryOrTooShort() { bad("l1:ae"); bad("i5e"); bad("d"); bad("") }

    @Test fun truncatedDocument() { bad("d4:name1:a"); bad("d4:infod4:name1:a") }

    @Test fun dataAfterTheEndIsRejected() { bad("de"+"x"); bad("d1:ai1eed1:ai1ee") }

    @Test fun stringLongerThanDataIsRejected() { bad("d4:name99:ae"); bad("d1:a2147483647:ae") }

    @Test fun hugeLengthDigitsAreRejectedWithoutOverflow() { bad("d1:a99999999999999999999:xe") }

    @Test fun keyWithoutValueAndNonStringKey() { bad("d3:fooe"); bad("di1e1:ae"); bad("dli1eei1ee") }

    @Test fun badIntegers() { bad("d1:ai03ee"); bad("d1:ai-0ee"); bad("d1:aiee"); bad("d1:ai12e"); bad("d1:ai-e") }

    @Test fun leadingZeroStringLength() { bad("d1:a02:xxe") }

    @Test fun veryDeepNestingFailsCleanlyInsteadOfOverflowingTheStack() {
        val deep = "d1:a" + "l".repeat(100_000) + "e".repeat(100_000) + "e"
        val r = v(deep)
        assertTrue(r is BencodeValidator.Result.Bad)
        assertEquals("nesting too deep", (r as BencodeValidator.Result.Bad).reason)
    }

    @Test fun nestingAtTheLimitIsAccepted() {
        val ok = "d1:a" + "l".repeat(BencodeValidator.MAX_DEPTH - 2) + "e".repeat(BencodeValidator.MAX_DEPTH - 2) + "e"
        assertEquals(BencodeValidator.Result.Ok, v(ok))
    }

    @Test fun binaryStringContentIsNotInterpreted() {
        assertEquals(BencodeValidator.Result.Ok, v("d6:pieces4:de\u0000ee1:ai1ee"))
    }
}
