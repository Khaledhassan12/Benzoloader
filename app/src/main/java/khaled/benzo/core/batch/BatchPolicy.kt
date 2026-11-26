package khaled.benzo.core.batch

/**
 * Batch loading: one link with $ marks becomes many links.
 * OBSERVED (video 3, left panel part 2, about 680 to 686 s): the dialog has a Link field, a numeric range "From 0 To 9 Step 1"
 * and a letter range "From A To Z", and a Help text: "$" marks one or more places where incrementing numbers or characters
 * are inserted; several $ in a row give leading zeros, so $$$ becomes 001.
 * INFERRED, not seen executed: Generate adds one download per value; every $ place gets the same value; letters are
 * single characters; from must not pass to. Limits and rejections below are my own safety choices.
 */
object BatchPolicy {
    /** Most links one batch may create. My own cap against a mistyped range; the reference limit is unknown. */
    const val MAX_LINKS = 1000

    sealed interface Result {
        data class Ok(val links: List<String>) : Result
        enum class Error : Result { NO_PLACE, NOT_WEB_LINK, BAD_NUMBER, BAD_STEP, BAD_LETTER, EMPTY_RANGE, TOO_MANY }
    }

    sealed interface Range {
        data class Numbers(val from: Long, val to: Long, val step: Long) : Range
        data class Letters(val from: Char, val to: Char) : Range
    }

    private val PLACE = Regex("\\$+")

    fun expand(template: String, range: Range): Result {
        val t = template.trim()
        if (!(t.startsWith("http://", true) || t.startsWith("https://", true))) return Result.Error.NOT_WEB_LINK
        val places = PLACE.findAll(t).toList()
        if (places.isEmpty()) return Result.Error.NO_PLACE
        val values: List<String> = when (range) {
            is Range.Numbers -> {
                if (range.from < 0 || range.to < 0) return Result.Error.BAD_NUMBER
                if (range.step < 1) return Result.Error.BAD_STEP
                if (range.from > range.to) return Result.Error.EMPTY_RANGE
                val count = (range.to - range.from) / range.step + 1
                if (count > MAX_LINKS) return Result.Error.TOO_MANY
                (0 until count).map { (range.from + it * range.step).toString() }
            }
            is Range.Letters -> {
                if (!(range.from.isLetter() && range.to.isLetter() && range.from.code < 128 && range.to.code < 128 && range.from.isUpperCase() == range.to.isUpperCase())) return Result.Error.BAD_LETTER
                if (range.from > range.to) return Result.Error.EMPTY_RANGE
                (range.from..range.to).map { it.toString() }
            }
        }
        val links = values.map { v ->
            PLACE.replace(t) { m -> if (range is Range.Numbers) v.padStart(m.value.length, '0') else v }
        }
        return Result.Ok(links.distinct())
    }
}
