package khaled.benzo.ui.settings

import android.content.Context
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import khaled.benzo.R
import khaled.benzo.core.catalog.FolderNames
import khaled.benzo.data.model.FileCategory

/** Lets the user rename the per-type folders used by cataloging. Blank fields mean the default name. */
object FolderNamesDialog {
    fun show(context: Context, current: FolderNames, onSave: (FolderNames) -> Unit) {
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
        }
        val fields = LinkedHashMap<FileCategory, EditText>()
        for (cat in FolderNames.editable) {
            box.addView(TextView(context).apply { text = FolderNames.defaults[cat] })
            val edit = EditText(context).apply {
                hint = FolderNames.defaults[cat]
                val own = current.nameFor(cat)
                if (own != FolderNames.defaults[cat]) setText(own)
                setSingleLine()
            }
            fields[cat] = edit
            box.addView(edit)
        }
        AlertDialog.Builder(context)
            .setTitle(R.string.pref_folder_names)
            .setView(ScrollView(context).apply { addView(box) })
            .setPositiveButton(R.string.reenter_save) { _, _ ->
                onSave(FolderNames.fromInput(fields.mapValues { it.value.text.toString() }))
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
