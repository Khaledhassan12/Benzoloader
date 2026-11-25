package khaled.benzo.core.security

import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypts and decrypts one text value. */
interface SecretCodec {
    fun encrypt(plain: String): String
    fun decrypt(token: String): String
}

/** AES-256-GCM with a random 12-byte IV per value (made by the cipher). The key comes from the caller (Android Keystore on the device). */
class AesGcmCodec(private val key: () -> SecretKey) : SecretCodec {
    override fun encrypt(plain: String): String {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        // The Android Keystore key requires randomized encryption: the cipher makes the 12-byte IV itself and a
        // caller-supplied IV is refused. The IV is read back and stored with the ciphertext. Tag length is the 128-bit default.
        c.init(Cipher.ENCRYPT_MODE, key())
        val iv = c.iv
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

/** Result of reading a stored column. [Unreadable] keeps the stored ciphertext so nothing is destroyed. */
sealed interface SecretRead {
    data class Ok(val value: String?) : SecretRead
    data class Unreadable(val raw: String) : SecretRead
}

/**
 * Column-level wrapper. Stored form is "enc1:" + codec output. A value without the prefix is a legacy plaintext row and is
 * returned as is until [SecretsMigration] seals it. A value that cannot be decrypted (key lost after a reinstall or backup
 * restore, or a Keystore fault) is reported as [SecretRead.Unreadable] with its raw stored text, so the caller can keep
 * it unchanged and show a re-entry status instead of treating it as empty.
 */
object Secrets {
    const val PREFIX = "enc1:"

    @Volatile private var codec: SecretCodec? = null

    fun install(c: SecretCodec) { codec = c }

    /** Fails loudly when no codec is installed: secrets are never written as plaintext by accident. */
    fun seal(plain: String?): String? = sealWith(requireCodec(), plain)

    fun read(stored: String?): SecretRead =
        if (stored == null || !stored.startsWith(PREFIX)) SecretRead.Ok(stored) else readWith(requireCodec(), stored)

    /** Value to store: new plaintext is sealed; when the caller has no new value but holds unreadable [raw], keep [raw]. */
    fun sealOrKeep(plain: String?, raw: String?): String? =
        if (plain == null && raw != null) raw else seal(plain)

    private fun requireCodec() = codec ?: error("SecretCodec not installed")

    fun sealWith(c: SecretCodec, plain: String?): String? =
        if (plain.isNullOrEmpty() || plain.startsWith(PREFIX)) plain else PREFIX + c.encrypt(plain)

    fun readWith(c: SecretCodec, stored: String?): SecretRead {
        if (stored == null || !stored.startsWith(PREFIX)) return SecretRead.Ok(stored)
        return runCatching { c.decrypt(stored.removePrefix(PREFIX)) }
            .fold({ SecretRead.Ok(it) }, { SecretRead.Unreadable(stored) })
    }
}
