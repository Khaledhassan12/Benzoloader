package khaled.benzo.core.files

import java.io.File
import java.security.MessageDigest
import java.util.Locale

/** Optional check of a finished file against a checksum the user typed. Algorithm is chosen by the hex length. */
object ChecksumPolicy {
    /** Cleaned lowercase hex, or null when it is not 32, 40 or 64 hex characters. */
    fun normalize(input: String?): String? {
        val t = input?.trim()?.lowercase(Locale.ROOT)?.replace(" ", "").orEmpty()
        if (t.length !in setOf(32, 40, 64)) return null
        return if (t.all { it in '0'..'9' || it in 'a'..'f' }) t else null
    }

    fun algorithm(hex: String): String? = when (hex.length) { 32 -> "MD5"; 40 -> "SHA-1"; 64 -> "SHA-256"; else -> null }

    fun hexOf(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }

    fun matches(file: File, expected: String): Boolean {
        val algo = algorithm(expected) ?: return false
        val md = MessageDigest.getInstance(algo)
        file.inputStream().buffered().use { s ->
            val buf = ByteArray(64 * 1024)
            while (true) { val n = s.read(buf); if (n < 0) break; md.update(buf, 0, n) }
        }
        return hexOf(md.digest()) == expected
    }
}
