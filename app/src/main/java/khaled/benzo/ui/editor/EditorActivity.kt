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
        setupListeners()
        handleIncomingIntent(autoAdd = savedInstanceState == null)
        // Video 8: a short sound plays when the Editor opens (matches the add sound by length and envelope; INFERRED, not an exact-audio claim).
        if (savedInstanceState == null && khaled.benzo.data.repository.SettingsRepository(this).soundOnComplete) {
            khaled.benzo.service.NotificationHelper(this).playSound(khaled.benzo.R.raw.add)
        }
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

    private fun handleIncomingIntent(autoAdd: Boolean) {
        val intentUrl = when (intent.action) {
            Intent.ACTION_VIEW -> intent.dataString
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            else -> intent.getStringExtra("extra_url")
        }

        // Only http and https are downloadable. The form is pre-filled only; nothing starts without the user.
        val clean = intentUrl?.trim()?.takeIf { it.startsWith("http://", true) || it.startsWith("https://", true) }
        if (!clean.isNullOrEmpty()) {
            binding.edtUrl.setText(clean)
            extractFileNameFromUrl(clean)
            // Only links from other apps (open or share) can skip the form, and only when the user turned that on.
            // The folder check inside submitDownload still runs; a problem shows the usual dialog on this screen.
            val external = intent.action == Intent.ACTION_VIEW || intent.action == Intent.ACTION_SEND
            val decision = khaled.benzo.core.queue.QuickAddPolicy.decide(
                viewModel.settingsRepository.quickAddLinks, viewModel.settingsRepository.quickAddStart, external)
            if (autoAdd && decision.skipEditor) binding.root.post { submitDownload(startNow = decision.startNow) }
        }
    }

    /** The editor is singleTask: a link shared while it is open arrives here and must not be dropped. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(autoAdd = true)
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

    private var folderChecked = false
    private var pendingStartNow = false

    /** Android 8 to 10: ask for the storage permission, then continue the add the user started. */
    private val legacyStorage = khaled.benzo.ui.common.LegacyStoragePermissionFlow(this) { submitDownload(pendingStartNow) }

    private fun showFolderProblem(verdict: khaled.benzo.core.storage.FolderCheck.Verdict, startNow: Boolean) {
        val appFolder = getExternalFilesDir(null)?.absolutePath
        val offerAccess = khaled.benzo.ui.common.AllFilesAccess.shouldOfferInDialog(this)
        val labels = ArrayList<String>()
        val actions = ArrayList<() -> Unit>()
        if (legacyStorage.offerInDialog()) {
            labels.add(getString(R.string.folder_dialog_allow_storage))
            actions.add { pendingStartNow = startNow; legacyStorage.start() }
        }
        if (offerAccess) {
            labels.add(getString(R.string.folder_dialog_allow_access))
            actions.add { khaled.benzo.ui.common.AllFilesAccess.explainAndRequest(this) }
        }
        if (appFolder != null) {
            labels.add(getString(R.string.folder_dialog_use_app_folder))
            actions.add {
                binding.edtPath.setText(appFolder)
                Toast.makeText(this, R.string.folder_app_folder_selected, Toast.LENGTH_LONG).show()
            }
        }
        labels.add(getString(R.string.folder_dialog_add_anyway))
        actions.add { folderChecked = true; submitDownload(startNow) }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.folder_dialog_title)
            .setMessage(getString(khaled.benzo.ui.common.FolderMessages.res(verdict)) +
                if (appFolder != null) "\n\n" + getString(R.string.folder_dialog_app_folder_note) else "")
            .setItems(labels.toTypedArray()) { _, i -> actions[i]() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
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

        // Check the folder as chosen. Nothing is changed unless the user picks the app folder in the dialog.
        if (!folderChecked) {
            val verdict = khaled.benzo.core.storage.FolderCheck.check(savePath, khaled.benzo.core.storage.AndroidFolderFs())
            if (verdict != khaled.benzo.core.storage.FolderCheck.Verdict.OK) {
                showFolderProblem(verdict, startNow)
                return
            }
        }
        folderChecked = false

        viewModel.addDownload(
            url = url,
            fileName = fileName,
            savePath = savePath,
            parts = binding.sliderThreads.value.toInt(),
            isWifiOnly = binding.chkWifiOnly.isChecked,
            userAgent = binding.edtUserAgent.text.toString().ifEmpty { null },
            referer = binding.edtReferer.text.toString().ifEmpty { null },
            cookies = binding.edtCookies.text.toString().ifEmpty { null },
            startNow = startNow,
            onError = { Toast.makeText(this, R.string.secrets_save_failed, Toast.LENGTH_LONG).show() }
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
