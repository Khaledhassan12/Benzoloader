package khaled.benzo.ui.main

import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import khaled.benzo.R

/**
 * File-picker side of list export and import. Must be created as a property of the activity
 * (the launchers register at construction). Reads and writes text only; list logic lives in core/data.
 */
class ListFileActions(
    private val activity: AppCompatActivity,
    private val exportText: () -> String,
    private val importText: (text: String, report: (added: Int, skipped: Int) -> Unit) -> Unit
) {
    private val exportLauncher = activity.registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            val text = prepared ?: exportText()
            prepared = null
            val ok = runCatching {
                activity.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) } != null
            }.getOrDefault(false)
            toast(if (ok) R.string.list_export_done else R.string.list_transfer_failed)
        }
    }

    private val importLauncher = activity.registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val text = runCatching {
                activity.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            }.getOrNull()
            if (text == null) toast(R.string.list_transfer_failed)
            else importText(text) { added, skipped ->
                Toast.makeText(activity, activity.getString(R.string.list_import_done, added, skipped), Toast.LENGTH_LONG).show()
            }
        }
    }

    private var prepared: String? = null

    private fun toast(res: Int) = Toast.makeText(activity, res, Toast.LENGTH_LONG).show()

    /** Exports already prepared text (the bundle chosen in the export dialog). */
    fun exportPrepared(text: String) { prepared = text; exportLauncher.launch("benzoloader-export.txt") }
    fun exportList() = exportLauncher.launch("benzoloader-list.txt")
    fun importList() = importLauncher.launch(arrayOf("text/plain", "text/*"))
}
