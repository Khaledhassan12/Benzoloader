package khaled.benzo.core.security

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Candidate way to carry secrets (site passwords, cookies, saved headers) in an export file:
 * the plain text is sealed with a key derived from a passphrase the user types. Pure JVM, no Android classes.
 * Format: `BZENV1:` iterations `:` salt `:` iv `:` ciphertext (Base64). AES-256-GCM, PBKDF2-HMAC-SHA256.
 * The passphrase is never stored. A wrong passphrase or a changed byte fails the GCM tag and returns null.
 * NOT wired into any export yet: using it is a decision for the user (see docs/EXPORT_SECRETS_DESIGN.md).
 */
object PassphraseEnvelope {
    private const val PREFIX = "BZENV1"
    const val DEFAULT_ITERATIONS = 310_000
    private const val MAX_ITERATIONS = 2_000_000
    private val rng = SecureRandom()

    private fun key(pass: CharArray, salt: ByteArray, iter: Int): SecretKeySpec {
        val spec = PBEKeySpec(pass, salt, iter, 256)
        try {
            val k = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return SecretKeySpec(k, "AES")
        } finally { spec.clearPassword() }
    }

    fun seal(plain: String, pass: CharArray, iterations: Int = DEFAULT_ITERATIONS): String {
        require(pass.isNotEmpty()) { "empty passphrase" }
        val salt = ByteArray(16).also(rng::nextBytes)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key(pass, salt, iterations))
        val ct = c.doFinal(plain.toByteArray(Charsets.UTF_8))
        val b = Base64.getEncoder()
        return listOf(PREFIX, iterations.toString(), b.encodeToString(salt), b.encodeToString(c.iv), b.encodeToString(ct)).joinToString(":")
    }

    /** Null for a wrong passphrase, damaged text, or an unreasonable iteration count. */
    fun open(token: String, pass: CharArray): String? = try {
        val p = token.trim().split(":")
        if (p.size != 5 || p[0] != PREFIX) null else {
            val iter = p[1].toInt()
            if (iter !in 1000..MAX_ITERATIONS) null else {
                val d = Base64.getDecoder()
                val c = Cipher.getInstance("AES/GCM/NoPadding")
                c.init(Cipher.DECRYPT_MODE, key(pass, d.decode(p[2]), iter), GCMParameterSpec(128, d.decode(p[3])))
                String(c.doFinal(d.decode(p[4])), Charsets.UTF_8)
            }
        }
    } catch (e: Exception) { null }
}
