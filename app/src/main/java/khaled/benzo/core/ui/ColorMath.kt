package khaled.benzo.core.ui

/**
 * Pure colour helpers for the shared colour picker (no Android classes, so they can be unit-tested).
 * Picker layout OBSERVED in the reference (video 5, about 330 s): a saturation/brightness square, a vertical hue bar, a
 * horizontal alpha slider, then an AARRGGBB hex box, an old and a new swatch, and a second hex box. Maths here is standard HSV.
 */
object ColorMath {
    data class Hsv(val h: Float, val s: Float, val v: Float)

    /** Hue 0..360, saturation 0..1, value 0..1 to opaque RGB packed with the given alpha 0..255. */
    fun toArgb(alpha: Int, h: Float, s: Float, v: Float): Int {
        val hh = ((h % 360f) + 360f) % 360f
        val c = v * s
        val x = c * (1f - kotlin.math.abs((hh / 60f) % 2f - 1f))
        val m = v - c
        val (r, g, b) = when {
            hh < 60f -> Triple(c, x, 0f)
            hh < 120f -> Triple(x, c, 0f)
            hh < 180f -> Triple(0f, c, x)
            hh < 240f -> Triple(0f, x, c)
            hh < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        fun ch(f: Float) = Math.round((f + m) * 255f).coerceIn(0, 255)
        return (alpha.coerceIn(0, 255) shl 24) or (ch(r) shl 16) or (ch(g) shl 8) or ch(b)
    }

    fun toHsv(argb: Int): Hsv {
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        val max = maxOf(r, g, b); val min = minOf(r, g, b); val d = max - min
        val h = when {
            d == 0f -> 0f
            max == r -> 60f * (((g - b) / d) % 6f)
            max == g -> 60f * ((b - r) / d + 2f)
            else -> 60f * ((r - g) / d + 4f)
        }
        return Hsv(((h % 360f) + 360f) % 360f, if (max == 0f) 0f else d / max, max)
    }

    fun alphaOf(argb: Int) = (argb ushr 24) and 0xFF

    /** Eight upper-case hex digits, AARRGGBB, no leading mark (as in the reference boxes). */
    fun format(argb: Int): String = "%08X".format(argb)

    /** Accepts AARRGGBB or RRGGBB (alpha FF), with or without a leading #. Null when it is anything else. */
    fun parse(text: String): Int? {
        val t = text.trim().removePrefix("#")
        if (t.any { !(it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F') }) return null
        return when (t.length) {
            8 -> t.toLong(16).toInt()
            6 -> (0xFF000000L or t.toLong(16)).toInt()
            else -> null
        }
    }
}
