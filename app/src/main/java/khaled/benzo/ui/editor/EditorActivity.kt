package khaled.benzo.ui.editor

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
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

        val editId = intent.getLongExtra(EXTRA_EDIT_ID, -1L)
        setupDefaultFields(skipClipboard = editId != -1L)
        setupCategory()
        setupProbe()
        setupListeners()
        if (editId != -1L) { beginEdit(editId); return }
        handleIncomingIntent(autoAdd = savedInstanceState == null)
        // Video 8: a short sound plays when the Editor opens (matches the add sound by length and envelope; INFERRED, not an exact-audio claim).
        if (savedInstanceState == null && khaled.benzo.data.repository.SettingsRepository(this).soundOnComplete) {
            khaled.benzo.service.NotificationHelper(this).playSound(khaled.benzo.R.raw.add)
        }
    }

    private fun setupDefaultFields(skipClipboard: Boolean = false) {
        val defaultPath = viewModel.settingsRepository.defaultDownloadPath.ifEmpty {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
        }
        binding.edtPath.setText(defaultPath)
        binding.sliderThreads.value = viewModel.settingsRepository.defaultPartsCount.toFloat()
        binding.txtThreadsValue.text = viewModel.settingsRepository.defaultPartsCount.toString()

        // Check clipboard for URL if empty
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (!skipClipboard && clipboard.hasPrimaryClip() && clipboard.primaryClipDescription?.hasMimeType("text/plain") == true) {
            val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
            if (!clipText.isNullOrEmpty() && (clipText.startsWith("http://") || clipText.startsWith("https://"))) {
                binding.edtUrl.setText(clipText)
                extractFileNameFromUrl(clipText)
            }
        }
    }

    /** First entry = automatic (from the file name, as before). A picked category is used as chosen; with Cataloging on it also picks the type folder. */
    private var pickedCategory: khaled.benzo.data.model.FileCategory? = null
    private val pickable = listOf(
        khaled.benzo.data.model.FileCategory.VIDEO to R.string.nav_video,
        khaled.benzo.data.model.FileCategory.MUSIC to R.string.nav_music,
        khaled.benzo.data.model.FileCategory.DOCUMENT to R.string.nav_documents,
        khaled.benzo.data.model.FileCategory.ARCHIVE to R.string.nav_archives,
        khaled.benzo.data.model.FileCategory.PROGRAM to R.string.nav_programs,
        khaled.benzo.data.model.FileCategory.IMAGE to R.string.nav_images,
        khaled.benzo.data.model.FileCategory.OTHER to R.string.nav_other
    )

    private var probeJob: kotlinx.coroutines.Job? = null
    private var nameEditedByUser = false
    private var settingNameFromCode = false
    /** Short timeouts so a slow or odd server cannot hold the form. Only headers are read: the response body is never read, even when the server ignores Range. */
    private val probeEngine by lazy {
        khaled.benzo.core.download.DownloadEngine(
            khaled.benzo.core.download.DownloadEngine.createDefaultClient { viewModel.settingsRepository.activeProxy() }.newBuilder()
                .connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS).readTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
                .callTimeout(10, java.util.concurrent.TimeUnit.SECONDS).build())
    }

    /**
     * When the link changes, wait a moment, then ask the server for size and suggested name (one small ranged request, no
     * cookies). The size is only shown. The name is filled only while the user has not typed their own; a typed name is never replaced.
     */
    private fun setupProbe() {
        binding.edtFileName.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { if (!settingNameFromCode) nameEditedByUser = true }
        })
        binding.edtUrl.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { scheduleProbe(s?.toString()?.trim().orEmpty()) }
        })
        scheduleProbe(binding.edtUrl.text.toString().trim())
    }

    private val probeGate = khaled.benzo.core.queue.ProbeGate()

    private fun scheduleProbe(url: String, force: Boolean = false) {
        if (!(url.startsWith("http://", true) || url.startsWith("https://", true))) {
            probeJob?.cancel(); binding.txtSizeLabel.text = ""; return
        }
        if (!probeGate.begin(url, force)) return
        probeJob?.cancel()
        binding.txtSizeLabel.setText(R.string.editor_size_checking)
        probeJob = androidx.lifecycle.lifecycleScope.launch {
            kotlinx.coroutines.delay(700)
            // Same network rules as the download itself: no request on no network or when a Wi-Fi-only rule is not met.
            val cond = khaled.benzo.service.DeviceConditions(this@EditorActivity, viewModel.settingsRepository).read(0)
            when (khaled.benzo.core.queue.ProbePolicy.decide(cond.networkAvailable, cond.onWifi, binding.chkWifiOnly.isChecked, cond.globalWifiOnly)) {
                khaled.benzo.core.queue.ProbePolicy.Decision.NO_NETWORK -> { binding.txtSizeLabel.setText(R.string.editor_size_no_network); return@launch }
                khaled.benzo.core.queue.ProbePolicy.Decision.WAITING_FOR_WIFI -> { binding.txtSizeLabel.setText(R.string.editor_size_wifi); return@launch }
                else -> {}
            }
            val ua = viewModel.settingsRepository.defaultUserAgent
            val headers = HashMap<String, String>()
            if (ua.isNotEmpty()) headers["User-Agent"] = ua
            val info = try { probeEngine.checkServer(url, headers) } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { null }
            // A newer link may have replaced this one while the request ran: drop the result then.
            ensureActive()
            if (!probeGate.isCurrent(url, binding.edtUrl.text.toString())) return@launch
            if (info == null) { binding.txtSizeLabel.setText(R.string.editor_size_unknown); return@launch }
            binding.txtSizeLabel.text = if (info.contentLength > 0)
                getString(R.string.editor_size_value, khaled.benzo.core.download.SpeedTracker.formatSize(info.contentLength))
                else getString(R.string.editor_size_unknown)
            val suggested = info.suggestedFileName
            if (!nameEditedByUser && !suggested.isNullOrBlank() && binding.edtFileName.text.isNullOrBlank()) {
                settingNameFromCode = true; binding.edtFileName.setText(suggested); settingNameFromCode = false
            }
        }
    }

    private var editing: khaled.benzo.data.model.DownloadItem? = null

    /**
     * Opens an existing item in the same form. Only "Change" exists here: it saves the edits and never restarts or
     * re-downloads. A separate Restart is NOT built (its meaning in the reference is inferred, and it would discard progress).
     */
    private fun beginEdit(id: Long) {
        binding.btnStart.visibility = View.GONE
        binding.btnQueue.setText(R.string.editor_change)
        binding.btnQueue.setOnClickListener { submitChange() }
        androidx.lifecycle.lifecycleScope.launch {
            val d = viewModel.load(id) ?: run { finish(); return@launch }
            editing = d
            nameEditedByUser = true
            probeGate.begin(d.url, false)
            binding.edtUrl.setText(d.url); binding.edtFileName.setText(d.fileName); binding.edtPath.setText(d.savePath)
            binding.sliderThreads.value = d.threadsCount.coerceIn(binding.sliderThreads.valueFrom.toInt(), binding.sliderThreads.valueTo.toInt()).toFloat()
            // The slider moves in steps of 64: a stored value off the step is shown at the nearest step (the stored value is unchanged until Change is pressed).
            binding.sliderItemSpeed.value = (Math.round(d.speedLimitKBps / 64f) * 64f).coerceIn(binding.sliderItemSpeed.valueFrom, binding.sliderItemSpeed.valueTo)
            binding.chkWifiOnly.isChecked = d.isWifiOnly
            binding.edtChecksum.setText(d.expectedChecksum.orEmpty())
            binding.edtCategory.setText(getString(pickable.first { it.first == d.category }.second), false)
            binding.layoutAdvanced.visibility = View.VISIBLE
        }
    }

    private fun submitChange() {
        val old = editing ?: return
        val url = binding.edtUrl.text.toString().trim()
        val name = binding.edtFileName.text.toString().trim()
        val path = binding.edtPath.text.toString().trim()
        if (!(url.startsWith("http://", true) || url.startsWith("https://", true) || old.isTorrent) || name.isEmpty()) {
            Toast.makeText(this, R.string.toast_invalid_url, Toast.LENGTH_SHORT).show(); return
        }
        val typed = binding.edtChecksum.text.toString()
        val checksum = khaled.benzo.core.files.ChecksumPolicy.normalize(typed)
        if (typed.isNotBlank() && checksum == null) { Toast.makeText(this, R.string.checksum_invalid, Toast.LENGTH_LONG).show(); return }
        if (old.isTorrent && checksum != null) { Toast.makeText(this, R.string.checksum_torrent_unsupported, Toast.LENGTH_LONG).show(); return }
        viewModel.changeDownload(old.id, url, name, path, binding.sliderThreads.value.toInt(), binding.chkWifiOnly.isChecked,
            pickedCategory, binding.sliderItemSpeed.value.toInt(), checksum) { result, later ->
            when (result) {
                EditorViewModel.ChangeResult.LOCKED -> Toast.makeText(this, R.string.editor_change_locked, Toast.LENGTH_LONG).show()
                EditorViewModel.ChangeResult.COLLISION -> Toast.makeText(this, R.string.editor_change_collision, Toast.LENGTH_LONG).show()
                EditorViewModel.ChangeResult.SAVED -> {
                    Toast.makeText(this, if (later) R.string.editor_change_saved_later else R.string.editor_change_saved, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun setupCategory() {
        val labels = listOf(getString(R.string.editor_category_auto)) + pickable.map { getString(it.second) }
        binding.edtCategory.setAdapter(android.widget.ArrayAdapter(this, android.R.layout.simple_list_item_1, labels))
        binding.edtCategory.setText(labels[0], false)
        binding.edtCategory.setOnItemClickListener { _, _, pos, _ ->
            pickedCategory = if (pos == 0) null else pickable[pos - 1].first
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

        binding.chkWifiOnly.setOnCheckedChangeListener { _, _ -> scheduleProbe(binding.edtUrl.text.toString().trim(), force = true) }
        binding.sliderItemSpeed.addOnChangeListener { _, v, _ ->
            binding.txtItemSpeedValue.text = if (v.toInt() == 0) getString(R.string.editor_speed_global) else "${v.toInt()} KB/s"
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

        val typedChecksum = binding.edtChecksum.text.toString()
        if (typedChecksum.isNotBlank() && khaled.benzo.core.files.ChecksumPolicy.normalize(typedChecksum) == null) {
            Toast.makeText(this, R.string.checksum_invalid, Toast.LENGTH_LONG).show()
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
            pickedCategory = pickedCategory,
            speedLimitKBps = binding.sliderItemSpeed.value.toInt(),
            expectedChecksum = khaled.benzo.core.files.ChecksumPolicy.normalize(binding.edtChecksum.text.toString()),
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
        const val EXTRA_EDIT_ID = "edit_download_id"
    }
}
