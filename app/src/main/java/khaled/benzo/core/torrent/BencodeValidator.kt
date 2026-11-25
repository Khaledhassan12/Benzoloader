package khaled.benzo.core.torrent

/**
 * Structural check of a bencoded document before it reaches the native torrent parser.
 * Iterative (no recursion, so deep nesting cannot overflow the stack), bounded, and strict:
 * one top-level dictionary, no trailing bytes, string lengths inside the buffer, well-formed integers,
 * dictionary keys are strings and every key has a value.
 * It does not replace libtorrent's own parsing and torrent checks, which still run afterwards.
 */
object BencodeValidator {
    const val MAX_DEPTH = 64
    const val MAX_TOKENS = 500_000

    sealed interface Result {
        object Ok : Result
        data class Bad(val reason: String, val offset: Int) : Result
    }

    private const val LIST = 0
    private const val DICT_KEY = 1
    private const val DICT_VALUE = 2

    fun validate(b: ByteArray): Result {
        val n = b.size
        if (n < 2) return Result.Bad("too short", 0)
        if (b[0] != 'd'.code.toByte()) return Result.Bad("top level is not a dictionary", 0)
        val stack = IntArray(MAX_DEPTH)
        var depth = 0
        var tokens = 0
        var i = 0

        fun valueDone() { if (depth > 0 && stack[depth - 1] == DICT_VALUE) stack[depth - 1] = DICT_KEY }

        while (i < n) {
            if (++tokens > MAX_TOKENS) return Result.Bad("too many items", i)
            val c = b[i].toInt().toChar()
            when {
                c == 'd' || c == 'l' -> {
                    if (depth > 0 && stack[depth - 1] == DICT_KEY) return Result.Bad("dictionary key is not a string", i)
                    if (depth >= MAX_DEPTH) return Result.Bad("nesting too deep", i)
                    stack[depth++] = if (c == 'd') DICT_KEY else LIST
                    i++
                }
                c == 'e' -> {
                    if (depth == 0) return Result.Bad("unexpected end marker", i)
                    if (stack[depth - 1] == DICT_VALUE) return Result.Bad("key without a value", i)
                    depth--
                    i++
                    valueDone()
                }
                c == 'i' -> {
                    if (depth > 0 && stack[depth - 1] == DICT_KEY) return Result.Bad("dictionary key is not a string", i)
                    var j = i + 1
                    if (j < n && b[j] == '-'.code.toByte()) j++
                    val digitsStart = j
                    while (j < n && b[j] in '0'.code.toByte()..'9'.code.toByte()) j++
                    val digits = j - digitsStart
                    if (digits == 0 || digits > 19) return Result.Bad("bad integer", i)
                    if (b[digitsStart] == '0'.code.toByte() && (digits > 1 || b[i + 1] == '-'.code.toByte())) return Result.Bad("bad integer", i)
                    if (j >= n || b[j] != 'e'.code.toByte()) return Result.Bad("integer not terminated", i)
                    i = j + 1
                    valueDone()
                }
                c in '0'..'9' -> {
                    var j = i
                    var len = 0L
                    while (j < n && b[j] in '0'.code.toByte()..'9'.code.toByte()) {
                        len = len * 10 + (b[j] - '0'.code.toByte())
                        if (len > Int.MAX_VALUE || j - i > 9) return Result.Bad("string length too large", i)
                        j++
                    }
                    if (b[i] == '0'.code.toByte() && j - i > 1) return Result.Bad("bad string length", i)
                    if (j >= n || b[j] != ':'.code.toByte()) return Result.Bad("string length not followed by colon", i)
                    val start = j + 1
                    if (len > n - start) return Result.Bad("string longer than the data", i)
                    i = start + len.toInt()
                    if (depth > 0 && stack[depth - 1] == DICT_KEY) stack[depth - 1] = DICT_VALUE else valueDone()
                }
                else -> return Result.Bad("unexpected byte", i)
            }
            if (depth == 0) {
                return if (i == n) Result.Ok else Result.Bad("data after the end of the document", i)
            }
        }
        return Result.Bad("document not closed", n)
    }
}
