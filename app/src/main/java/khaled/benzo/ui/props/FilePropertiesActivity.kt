package khaled.benzo.ui.props

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import khaled.benzo.R
import khaled.benzo.data.repository.DownloadRepository
import khaled.benzo.databinding.ActivityFilePropertiesBinding
import kotlinx.coroutines.launch

class FilePropertiesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFilePropertiesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFilePropertiesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val downloadId = intent.getLongExtra("download_id", -1L)
        if (downloadId != -1L) {
            val repository = DownloadRepository(this)
            lifecycleScope.launch {
                val item = repository.getDownload(downloadId)
                if (item != null) {
                    binding.txtPropFileName.text = item.fileName
                    binding.txtPropUrl.text = "URL: ${item.url}"
                    binding.txtPropPath.text = "Path: ${item.savePath}/${item.fileName}"
                    binding.txtPropSize.text = "Size: ${item.downloadedBytes} / ${if (item.totalBytes > 0) item.totalBytes else "Unknown"} bytes"
                    binding.txtPropParts.text = "Threads: ${item.parts}"
                    val hash = item.torrentInfoHash
                    if (item.isTorrent && hash != null) {
                        binding.btnTrackers.visibility = android.view.View.VISIBLE
                        binding.btnTrackers.setOnClickListener { showTrackers(hash) }
                    }
                }
            }
        }

        binding.btnCloseProperties.setOnClickListener {
            finish()
        }
    }

    /** Edits the tracker list of a running torrent, one address per line. Session only: see TorrentEngine.replaceTrackers. */
    private fun showTrackers(infoHash: String) {
        val engine = khaled.benzo.core.torrent.TorrentEngine.orNull()
        val current = engine?.trackers(infoHash)
        if (engine == null || current == null) {
            android.widget.Toast.makeText(this, R.string.trackers_not_running, android.widget.Toast.LENGTH_LONG).show()
            return
        }
        val edit = android.widget.EditText(this).apply {
            setText(current.joinToString("\n"))
            minLines = 5
            gravity = android.view.Gravity.TOP
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or android.text.InputType.TYPE_TEXT_VARIATION_URI
        }
        val pad = (24 * resources.displayMetrics.density).toInt()
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.trackers_title)
            .setMessage(R.string.trackers_note)
            .setView(android.widget.FrameLayout(this).apply { setPadding(pad, 0, pad, 0); addView(edit) })
            .setPositiveButton(R.string.reenter_save) { _, _ ->
                when (val r = khaled.benzo.core.torrent.TrackerPolicy.parse(edit.text.toString())) {
                    is khaled.benzo.core.torrent.TrackerPolicy.Parsed.Bad ->
                        android.widget.Toast.makeText(this, getString(R.string.trackers_bad, r.line), android.widget.Toast.LENGTH_LONG).show()
                    is khaled.benzo.core.torrent.TrackerPolicy.Parsed.Ok ->
                        if (!engine.replaceTrackers(infoHash, r.urls))
                            android.widget.Toast.makeText(this, R.string.trackers_not_running, android.widget.Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
