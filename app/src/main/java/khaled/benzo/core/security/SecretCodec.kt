package khaled.benzo.core.security

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypts and decrypts one text value. */
interface SecretCodec {
    fun encrypt(plain: String): String
    fun decrypt(token: String): String
}

/** AES-256-GCM with a random 12-byte IV per value. The key comes from the caller (Android Keystore on the device). */
class AesGcmCodec(private val key: () -> SecretKey) : SecretCodec {
    private val rng = SecureRandom()

    override fun encrypt(plain: String): String {
        val iv = ByteArray(12).also { rng.nextBytes(it) }
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key(), GCMParameterSpec(128, iv))
        val ct = c.doFinal(plain.toByteArray(Charsets.UTF_8))
        val b64 = Base64.getEncoder()
        return b64.encodeToString(iv) + ":" + b64.encodeToString(ct)
    }

    override fun decrypt(token: String): String {
        val (ivs, cts) = token.split(":", limit = 2)
        val d = Base64.getDecoder()
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, d.decode(ivs)))
        return String(c.doFinal(d.decode(cts)), Charsets.UTF_8)
    }
}

/**
 * Column-level wrapper. Stored form is "enc1:" + codec output. A value without the prefix is a legacy plaintext row and is
 * returned as is, then re-encrypted the next time the row is written. A value that cannot be decrypted (for example the
 * key was lost after a reinstall or backup restore) reads as null instead of crashing.
 */
object Secrets {
    const val PREFIX = "enc1:"

    @Volatile private var codec: SecretCodec? = null

    fun install(c: SecretCodec) { codec = c }

    /** Fails loudly when no codec is installed: secrets are never written as plaintext by accident. */
    fun seal(plain: String?): String? = sealWith(requireCodec(), plain)

    fun open(stored: String?): String? =
        if (stored == null || !stored.startsWith(PREFIX)) stored else openWith(requireCodec(), stored)

    private fun requireCodec() = codec ?: error("SecretCodec not installed")

    fun sealWith(c: SecretCodec, plain: String?): String? =
        if (plain.isNullOrEmpty() || plain.startsWith(PREFIX)) plain else PREFIX + c.encrypt(plain)

    fun openWith(c: SecretCodec, stored: String?): String? {
        if (stored == null || !stored.startsWith(PREFIX)) return stored
        return runCatching { c.decrypt(stored.removePrefix(PREFIX)) }.getOrNull()
    }
}
