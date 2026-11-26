package khaled.benzo.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import khaled.benzo.data.model.DownloadItem
import java.io.File

/** Opening and sharing a finished file through the system chooser. The file is handed over with a temporary read grant, never by path. */
object ItemFileIntents {
    fun file(item: DownloadItem) = File(item.savePath, item.fileName)

    private fun uri(c: Context, item: DownloadItem): Uri = FileProvider.getUriForFile(c, "khaled.benzo.fileprovider", file(item))

    fun mime(item: DownloadItem): String =
        item.mimeType?.takeIf { it.isNotBlank() }
            ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(item.fileName.substringAfterLast('.', "").lowercase())
            ?: "*/*"

    fun open(c: Context, item: DownloadItem): Intent = Intent.createChooser(
        Intent(Intent.ACTION_VIEW).setDataAndType(uri(c, item), mime(item)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), null)

    /** Plain text with the links, handed to the system chooser. The user has already seen this exact text. */
    fun sendLinks(links: List<String>): Intent =
        Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, links.joinToString("\n")), null)

    fun send(c: Context, items: List<DownloadItem>): Intent {
        val i = if (items.size == 1) Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uri(c, items[0])).setType(mime(items[0]))
        else Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(items.map { uri(c, it) })).setType("*/*")
        return Intent.createChooser(i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), null)
    }
}
