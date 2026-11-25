package khaled.benzo.core.security

import android.content.Context
import khaled.benzo.data.db.BenzoDatabase

/** Runs [SecretsMigration] over downloads and sites at app start. Never throws and never logs values. */
class SecretsMigrationRunner(private val context: Context, private val codec: SecretCodec) {

    /** Returns how many rows were sealed. Rows that fail stay as they were and are tried again next start. */
    suspend fun run(): Int {
        val db = BenzoDatabase.getInstance(context)
        var done = 0
        for (row in runCatching { db.downloadDao().getAllOnce() }.getOrDefault(emptyList())) {
            val n = SecretsMigration.plan(codec, row.cookies, row.headers) ?: continue
            done += runCatching { db.downloadDao().swapSecrets(row.id, row.cookies, row.headers, n[0], n[1]) }.getOrDefault(0)
        }
        for (row in runCatching { db.siteDao().getAllSites() }.getOrDefault(emptyList())) {
            val n = SecretsMigration.plan(codec, row.password, row.customCookies) ?: continue
            done += runCatching { db.siteDao().swapSecrets(row.id, row.password, row.customCookies, n[0], n[1]) }.getOrDefault(0)
        }
        return done
    }
}
