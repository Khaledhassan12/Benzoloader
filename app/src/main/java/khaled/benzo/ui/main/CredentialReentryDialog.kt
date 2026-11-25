package khaled.benzo.ui.main

import android.content.Context
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import khaled.benzo.R

/** Asks for the cookies and headers of a download whose saved values cannot be read. Download progress is kept. */
object CredentialReentryDialog {
    fun show(
        context: Context,
        askCookies: Boolean,
        askHeaders: Boolean,
        onSave: (cookies: String?, headers: String?) -> Unit,
        onDiscard: () -> Unit
    ) {
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
        }
        val cookies = EditText(context).apply { hint = context.getString(R.string.reenter_cookies_hint) }
        val headers = EditText(context).apply { hint = context.getString(R.string.reenter_headers_hint) }
        if (askCookies) box.addView(cookies)
        if (askHeaders) box.addView(headers)
        AlertDialog.Builder(context)
            .setTitle(R.string.reenter_title)
            .setMessage(R.string.reenter_message)
            .setView(box)
            .setPositiveButton(R.string.reenter_save) { _, _ ->
                onSave(cookies.text.toString().ifBlank { null }, headers.text.toString().ifBlank { null })
            }
            .setNeutralButton(R.string.reenter_discard) { _, _ -> onDiscard() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
