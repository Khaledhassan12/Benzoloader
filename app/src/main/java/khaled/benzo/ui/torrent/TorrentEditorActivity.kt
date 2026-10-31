package khaled.benzo.ui.torrent

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import khaled.benzo.R
import khaled.benzo.databinding.ActivityTorrentEditorBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TorrentEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTorrentEditorBinding
    private val viewModel: TorrentEditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTorrentEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        handleIntentData()
        setupListeners()
    }

    private fun setupRecyclerView() {
        binding.recyclerViewTorrentFiles.layoutManager = LinearLayoutManager(this)
    }

    private fun handleIntentData() {
        val magnetOrUri = intent.dataString ?: intent.getStringExtra("torrent_path")
        if (!magnetOrUri.isNullOrEmpty()) {
            binding.txtTorrentName.text = magnetOrUri.substringAfterLast('/')
            viewModel.loadTorrent(magnetOrUri)
        }

        lifecycleScope.launch {
            viewModel.torrentFiles.collectLatest { files ->
                binding.recyclerViewTorrentFiles.adapter = TorrentFileAdapter(files)
                val totalBytes = files.sumOf { it.size }
                binding.txtTorrentStats.text = "Files: ${files.size} · Size: ${totalBytes / (1024 * 1024)} MB"
            }
        }
    }

    private fun setupListeners() {
        binding.btnCancelTorrent.setOnClickListener {
            finish()
        }

        binding.btnStartTorrent.setOnClickListener {
            val defaultPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
            val infoHash = intent.dataString ?: "sample_hash"
            viewModel.startDownload(infoHash, defaultPath)
            Toast.makeText(this, R.string.toast_download_added, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
