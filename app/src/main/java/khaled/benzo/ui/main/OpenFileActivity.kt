package khaled.benzo.ui.main

import android.app.Activity
import android.content.ActivityNotFoundException
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import khaled.benzo.R
import khaled.benzo.data.repository.DownloadRepository
import kotlinx.coroutines.launch

/**
 * Target of the finished-download notification. The file is looked up when the user taps, not when the notification was
 * made, so a file deleted or moved since then gives a message instead of a dead tap. Not exported: other apps cannot start it.
 */
class OpenFileActivity : androidx.appcompat.app.AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getLongExtra("download_id", -1L)
        lifecycleScope.launch {
            val item = if (id == -1L) null else DownloadRepository(applicationContext).getDownload(id)
            if (item == null || !ItemFileIntents.file(item).isFile) {
                Toast.makeText(this@OpenFileActivity, R.string.sel_no_file_now, Toast.LENGTH_LONG).show()
            } else {
                try { startActivity(ItemFileIntents.open(this@OpenFileActivity, item)) }
                catch (e: ActivityNotFoundException) { Toast.makeText(this@OpenFileActivity, R.string.sel_no_app, Toast.LENGTH_LONG).show() }
            }
            finish()
        }
    }
}
