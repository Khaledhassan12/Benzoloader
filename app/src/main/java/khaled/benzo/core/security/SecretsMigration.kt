package khaled.benzo.core.security

/**
 * Seals legacy plaintext columns. Safe to repeat: rows already sealed are skipped, every row is updated on its own with a
 * compare-and-set on the old values, and a sealed value is read back before it replaces the plaintext. If the app dies
 * half way, the rows not yet done stay readable plaintext and the next start continues.
 */
object SecretsMigration {
    /** New values for one row, or null when the row needs no change. Any failure leaves the row untouched. */
    fun plan(c: SecretCodec, vararg stored: String?): List<String?>? {
        if (stored.none { needsSealing(it) }) return null
        return try {
            stored.map { v ->
                if (!needsSealing(v)) v else {
                    val sealed = Secrets.sealWith(c, v) ?: return null
                    val back = Secrets.readWith(c, sealed)
                    if (back !is SecretRead.Ok || back.value != v) return null
                    sealed
                }
            }
        } catch (e: Exception) {
            null // for example a Keystore fault: leave the row as it is and try again next start
        }
    }

    fun needsSealing(v: String?) = !v.isNullOrEmpty() && !v.startsWith(Secrets.PREFIX)
}
