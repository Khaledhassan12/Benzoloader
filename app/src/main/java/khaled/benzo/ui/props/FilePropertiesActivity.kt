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
    private var rows: List<PropRow> = emptyList()
    private var editing = false
    private val prefs by lazy { getSharedPreferences("properties_ui", MODE_PRIVATE) }
    private fun hidden(): Set<String> = prefs.getStringSet("hidden", emptySet()) ?: emptySet()

    private fun label(key: String): String = getString(when (key) {
        "name" -> R.string.prop_name; "category" -> R.string.prop_category; "path" -> R.string.prop_path
        "url" -> R.string.prop_url; "size" -> R.string.prop_size; "downloaded" -> R.string.prop_downloaded
        "speed" -> R.string.prop_speed; "message" -> R.string.prop_message; "threads" -> R.string.prop_threads
        "limit" -> R.string.prop_limit; "checksum" -> R.string.prop_checksum; "created" -> R.string.prop_created
        "finished" -> R.string.prop_finished; "hash" -> R.string.prop_hash; else -> R.string.prop_agent })

    /** Eye mode shows a checkbox beside every row. Unchecked rows are hidden on this screen; the choice is kept on the device. */
    private fun renderRows() {
        val box = binding.propRows; box.removeAllViews()
        val hide = hidden()
        val shown = if (editing) rows else PropertyRows.visible(rows, hide)
        val dp = resources.displayMetrics.density
        for (r in shown) {
            val line = android.widget.LinearLayout(this).apply { orientation = android.widget.LinearLayout.HORIZONTAL; setPadding(0, (6 * dp).toInt(), 0, (6 * dp).toInt()) }
            if (editing) line.addView(android.widget.CheckBox(this).apply {
                isChecked = r.key !in hide
                setOnCheckedChangeListener { _, on ->
                    val now = hidden().toMutableSet(); if (on) now.remove(r.key) else now.add(r.key)
                    prefs.edit().putStringSet("hidden", now).apply()
                }
            })
            val col = android.widget.LinearLayout(this).apply { orientation = android.widget.LinearLayout.VERTICAL }
            col.addView(android.widget.TextView(this).apply { text = label(r.key); textSize = 12f; alpha = 0.7f })
            col.addView(android.widget.TextView(this).apply { text = r.value; textSize = 15f; setTextIsSelectable(true) })
            line.addView(col); box.addView(line)
        }
    }

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
                    rows = PropertyRows.build(item, { khaled.benzo.core.download.SpeedTracker.formatSize(it) },
                        { java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it)) })
                    renderRows()
                    val hash = item.torrentInfoHash
                    if (item.isTorrent && hash != null) {
                        binding.btnTrackers.visibility = android.view.View.VISIBLE
                        binding.btnTrackers.setOnClickListener { showTrackers(hash) }
                    }
                }
            }
        }

        binding.btnPropVisibility.setOnClickListener { editing = !editing; renderRows() }
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
