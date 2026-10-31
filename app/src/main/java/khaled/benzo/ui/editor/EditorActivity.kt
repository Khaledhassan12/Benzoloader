package khaled.benzo.ui.editor

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import khaled.benzo.R
import khaled.benzo.databinding.ActivityEditorBinding
import khaled.benzo.service.DownloadService
import khaled.benzo.ui.common.PathPickerActivity

class EditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditorBinding
    private val viewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDefaultFields()
        handleIncomingIntent()
        setupListeners()
    }

    private fun setupDefaultFields() {
        val defaultPath = viewModel.settingsRepository.defaultDownloadPath.ifEmpty {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
        }
        binding.edtPath.setText(defaultPath)
        binding.sliderThreads.value = viewModel.settingsRepository.defaultPartsCount.toFloat()
        binding.txtThreadsValue.text = viewModel.settingsRepository.defaultPartsCount.toString()

        // Check clipboard for URL if empty
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (clipboard.hasPrimaryClip() && clipboard.primaryClipDescription?.hasMimeType("text/plain") == true) {
            val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
            if (!clipText.isNullOrEmpty() && (clipText.startsWith("http://") || clipText.startsWith("https://"))) {
                binding.edtUrl.setText(clipText)
                extractFileNameFromUrl(clipText)
            }
        }
    }

    private fun handleIncomingIntent() {
        val intentUrl = when (intent.action) {
            Intent.ACTION_VIEW -> intent.dataString
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            else -> intent.getStringExtra("extra_url")
        }

        if (!intentUrl.isNullOrEmpty()) {
            binding.edtUrl.setText(intentUrl)
            extractFileNameFromUrl(intentUrl)
        }
    }

    private fun extractFileNameFromUrl(url: String) {
        try {
            val uri = Uri.parse(url)
            val path = uri.path
            if (!path.isNullOrEmpty()) {
                val name = path.substringAfterLast('/')
                if (name.isNotEmpty() && name.contains('.')) {
                    binding.edtFileName.setText(name)
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun setupListeners() {
        binding.sliderThreads.addOnChangeListener { _, value, _ ->
            binding.txtThreadsValue.text = value.toInt().toString()
        }

        binding.btnToggleAdvanced.setOnClickListener {
            binding.layoutAdvanced.visibility = if (binding.layoutAdvanced.visibility == View.VISIBLE) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }

        binding.btnSelectPath.setOnClickListener {
            val intent = Intent(this, PathPickerActivity::class.java)
            startActivityForResult(intent, REQUEST_PATH)
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }

        binding.btnQueue.setOnClickListener {
            submitDownload(startNow = false)
        }

        binding.btnStart.setOnClickListener {
            submitDownload(startNow = true)
        }
    }

    private fun submitDownload(startNow: Boolean) {
        val url = binding.edtUrl.text.toString().trim()
        var fileName = binding.edtFileName.text.toString().trim()
        val savePath = binding.edtPath.text.toString().trim()

        if (url.isEmpty()) {
            Toast.makeText(this, R.string.toast_invalid_url, Toast.LENGTH_SHORT).show()
            return
        }

        if (fileName.isEmpty()) {
            fileName = "download_" + System.currentTimeMillis()
        }

        viewModel.addDownload(
            url = url,
            fileName = fileName,
            savePath = savePath,
            parts = binding.sliderThreads.value.toInt(),
            isWifiOnly = binding.chkWifiOnly.isChecked,
            userAgent = binding.edtUserAgent.text.toString().ifEmpty { null },
            referer = binding.edtReferer.text.toString().ifEmpty { null },
            cookies = binding.edtCookies.text.toString().ifEmpty { null },
            startNow = startNow
        ) { downloadId ->
            if (startNow) {
                val serviceIntent = Intent(this, DownloadService::class.java).apply {
                    action = DownloadService.ACTION_START_DOWNLOAD
                    putExtra("download_id", downloadId)
                }
                startService(serviceIntent)
            }
            Toast.makeText(this, R.string.toast_download_added, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PATH && resultCode == RESULT_OK) {
            val selectedPath = data?.getStringExtra("selected_path")
            if (!selectedPath.isNullOrEmpty()) {
                binding.edtPath.setText(selectedPath)
            }
        }
    }

    companion object {
        private const val REQUEST_PATH = 101
    }
}
