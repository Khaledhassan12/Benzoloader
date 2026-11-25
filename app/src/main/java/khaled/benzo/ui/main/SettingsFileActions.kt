package khaled.benzo.ui.main

import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import khaled.benzo.R
import khaled.benzo.core.settings.SettingsBackup
import khaled.benzo.data.repository.SettingsRepository

/**
 * File-picker side of settings export and import. Create as a property of the activity (launchers register at construction).
 * The file holds app settings only (see SettingsBackup); the import checks every value before writing any of them.
 */
class SettingsFileActions(
    private val activity: AppCompatActivity,
    private val settingsProvider: () -> SettingsRepository,
    private val afterImport: () -> Unit
) {
    private val exportLauncher = activity.registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            val text = SettingsBackup.format(settingsProvider().backupValues())
            val ok = runCatching {
                activity.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) } != null
            }.getOrDefault(false)
            toast(if (ok) activity.getString(R.string.settings_export_done) else activity.getString(R.string.list_transfer_failed))
        }
    }

    private val importLauncher = activity.registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val text = runCatching {
                activity.contentResolver.openInputStream(uri)?.use { s ->
                    val buf = java.io.ByteArrayOutputStream()
                    val chunk = ByteArray(4096)
                    var tooBig = false
                    while (true) {
                        val n = s.read(chunk)
                        if (n < 0) break
                        buf.write(chunk, 0, n)
                        if (buf.size() > MAX_BYTES) { tooBig = true; break }
                    }
                    if (tooBig) null else buf.toByteArray().toString(Charsets.UTF_8)
                }
            }.getOrNull()
            if (text == null) { toast(activity.getString(R.string.list_transfer_failed)); return@registerForActivityResult }
            val r = SettingsBackup.parse(text)
            if (!r.isSettingsFile || r.accepted.isEmpty()) { toast(activity.getString(R.string.settings_import_not_settings)); return@registerForActivityResult }
            val pathSkipped = settingsProvider().applyBackup(r.accepted) { p -> File(p).let { it.isDirectory && it.canWrite() } }
            val msg = activity.getString(R.string.settings_import_done, r.accepted.size - (if (pathSkipped) 1 else 0), r.invalid + r.ignored) +
                if (pathSkipped) "\n" + activity.getString(R.string.settings_import_folder_skipped) else ""
            toast(msg)
            afterImport()
        }
    }

    private fun toast(msg: String) = Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()

    fun exportSettings() = exportLauncher.launch("benzoloader-settings.txt")
    fun importSettings() = importLauncher.launch(arrayOf("text/plain", "text/*"))

    private companion object { const val MAX_BYTES = 64 * 1024 }
}
