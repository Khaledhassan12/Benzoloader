package khaled.benzo.ui.settings

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import khaled.benzo.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbarSettings.setNavigationOnClickListener {
            saveAndFinish()
        }

        loadCurrentSettings()
        showOnlyRequestedSection()
        bindDependents(binding.switchTorrentSeed, binding.txtSeedRatio, binding.sliderSeedRatio, binding.txtSeedMinutes, binding.sliderSeedMinutes)
        bindDependents(binding.switchCatalog, binding.catalogCategoryList)
        bindDependents(binding.switchSchedule, binding.btnScheduleStart, binding.btnScheduleStop)
    }

    /** Rows that only matter while [parent] is on are faded and disabled when it is off. Their values are still saved. */
    private fun bindDependents(parent: android.widget.CompoundButton, vararg rows: android.view.View) {
        fun set(v: android.view.View, on: Boolean) {
            v.isEnabled = on
            (v as? android.view.ViewGroup)?.let { g -> for (i in 0 until g.childCount) set(g.getChildAt(i), on) }
        }
        fun apply(on: Boolean) = rows.forEach { set(it, on); it.alpha = if (on) 1f else 0.38f }
        apply(parent.isChecked)
        parent.setOnCheckedChangeListener { _, on -> apply(on) }
    }

    /** With [EXTRA_SECTION] only that category is shown (the hub opens it this way); without it every category stays visible. */
    private fun showOnlyRequestedSection() {
        val wanted = SettingsSection.fromId(intent.getStringExtra(EXTRA_SECTION)) ?: return
        for (s in SettingsSection.entries) {
            val v = findViewById<android.view.View>(resources.getIdentifier("section_" + s.id, "id", packageName))
            v?.visibility = if (s == wanted) android.view.View.VISIBLE else android.view.View.GONE
        }
        binding.toolbarSettings.setTitle(wanted.title)
    }

    companion object { const val EXTRA_SECTION = "section" }

    private var startMinute = 0
    private var stopMinute = 0

    private fun fmt(minute: Int) = "%02d:%02d".format(minute / 60, minute % 60)

    private fun updateScheduleLabels() {
        binding.btnScheduleStart.text = getString(khaled.benzo.R.string.pref_schedule_start, fmt(startMinute))
        binding.btnScheduleStop.text = getString(khaled.benzo.R.string.pref_schedule_stop, fmt(stopMinute))
    }

    private fun updateSeedLabels() = refreshValueRows()

    private fun updateSpeedLabel() = refreshValueRows()

    private fun pickTime(current: Int, onPicked: (Int) -> Unit) {
        android.app.TimePickerDialog(this, { _, h, m -> onPicked(h * 60 + m) },
            current / 60, current % 60, true).show()
    }

    private fun updateConnectionsLabel() = refreshValueRows()

    private fun updateBatteryLabel() = refreshValueRows()

    /** One row per cataloging category, each opens its own screen. Rebuilt in onResume so renamed folders show. */
    private fun buildCatalogRows(names: khaled.benzo.core.catalog.FolderNames) {
        val list = binding.catalogCategoryList
        list.removeAllViews()
        for (cat in khaled.benzo.core.catalog.FolderNames.editable) {
            val row = layoutInflater.inflate(khaled.benzo.R.layout.item_nav_row, list, false)
            row.findViewById<android.widget.TextView>(khaled.benzo.R.id.navTitle).text = names.nameFor(cat)
            row.setOnClickListener {
                startActivity(android.content.Intent(this, CatalogCategoryActivity::class.java).putExtra(CatalogCategoryActivity.EXTRA_CATEGORY, cat.key))
            }
            row.isEnabled = binding.switchCatalog.isChecked
            list.addView(row)
        }
    }

    private var userAgentValue: String = ""

    /** Title plus the current value (or "Default") in the accent colour; tapping opens a text dialog. Saved with the rest on exit. */
    private fun showUserAgentRow(value: String) {
        userAgentValue = value
        val row = binding.txtUserAgent
        val title = getString(khaled.benzo.R.string.row_user_agent)
        val shown = value.ifBlank { getString(khaled.benzo.R.string.val_default) }
        val b = android.text.SpannableStringBuilder(title).append('\n')
        val start = b.length
        b.append(shown)
        b.setSpan(android.text.style.ForegroundColorSpan(com.google.android.material.color.MaterialColors.getColor(row, com.google.android.material.R.attr.colorPrimary)), start, b.length, 0)
        row.text = b
        val d = resources.displayMetrics.density
        row.minHeight = (72 * d).toInt(); row.gravity = android.view.Gravity.CENTER_VERTICAL
        row.setPadding((20 * d).toInt(), (12 * d).toInt(), (20 * d).toInt(), (12 * d).toInt())
        row.isClickable = true
        row.setOnClickListener {
            val edit = android.widget.EditText(this).apply { setText(userAgentValue); hint = getString(khaled.benzo.R.string.val_default); setSingleLine() }
            val pad = (24 * d).toInt()
            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(getString(khaled.benzo.R.string.sum_user_agent))
                .setView(android.widget.FrameLayout(this).apply { setPadding(pad, 0, pad, 0); addView(edit) })
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val t = edit.text.toString().trim()
                    if (t.all { it.code in 32..126 }) showUserAgentRow(t)
                    else android.widget.Toast.makeText(this, khaled.benzo.R.string.ua_invalid, android.widget.Toast.LENGTH_LONG).show()
                }
                .setNeutralButton(khaled.benzo.R.string.val_default) { _, _ -> showUserAgentRow("") }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private var valueRows: List<ValueRow> = emptyList()
    private fun refreshValueRows() = valueRows.forEach { it.refresh() }

    private fun setupValueRows() {
        val g = { id: Int -> getString(id) }
        val R = khaled.benzo.R.string
        valueRows = listOf(
            ValueRow(this, binding.txtMaxDownloads, binding.sliderMaxDownloads, g(R.row_max_downloads), { getString(R.val_count, it) }),
            ValueRow(this, binding.txtDefaultThreads, binding.sliderDefaultThreads, g(R.row_threads), { getString(R.val_threads, it) }),
            ValueRow(this, binding.txtMinThread, binding.sliderMinThread, g(R.row_min_thread), { if (it >= 1024) "%.1f MB".format(it / 1024f) else "$it KB" }),
            ValueRow(this, binding.txtVibSuccess, binding.sliderVibSuccess, g(R.row_vib_success), { getString(R.val_ms, it) }),
            ValueRow(this, binding.txtVibError, binding.sliderVibError, g(R.row_vib_error), { getString(R.val_ms, it) }),
            ValueRow(this, binding.txtErrorRetries, binding.sliderErrorRetries, g(R.row_retries), { getString(R.val_count, it) }),
            ValueRow(this, binding.txtSpeedLimit, binding.sliderSpeedLimit, g(R.row_speed),
                { if (it == 0) g(R.pref_speed_unlimited) else getString(R.val_kbps, it) }),
            ValueRow(this, binding.txtBatteryThreshold, binding.sliderBattery, g(R.row_battery), { getString(R.val_percent, it) },
                { getString(R.pref_battery_threshold_fmt, it, khaled.benzo.core.queue.QueuePolicy.restartBelowOrAt(it) + 1) }),
            ValueRow(this, binding.txtSeedRatio, binding.sliderSeedRatio, g(R.row_seed_ratio),
                { if (it == 0) g(R.pref_unlimited) else "%d.%d".format(it / 10, it % 10) }),
            ValueRow(this, binding.txtSeedMinutes, binding.sliderSeedMinutes, g(R.row_seed_time),
                { if (it == 0) g(R.pref_unlimited) else "%d:%02d".format(it / 60, it % 60) }),
            ValueRow(this, binding.txtTorrentConnections, binding.sliderTorrentConnections, g(R.row_connections),
                { if (it == 0) g(R.pref_torrent_connections_default) else it.toString() })
        )
    }

    private fun loadCurrentSettings() {
        val repo = viewModel.repository
        setupValueRows()
        binding.sliderMaxDownloads.value = repo.maxConcurrentDownloads.toFloat()
        binding.sliderDefaultThreads.value = repo.defaultPartsCount.toFloat()
        binding.switchWifiOnly.isChecked = repo.isWifiOnly
        binding.sliderBattery.value = repo.batteryThresholdPercent.toFloat()
        binding.switchStopOnUnplug.isChecked = repo.stopOnChargerUnplug
        binding.sliderSpeedLimit.value = repo.speedLimitKBps.toFloat().coerceIn(0f, 10240f)
        updateSpeedLabel()
        binding.sliderSpeedLimit.addOnChangeListener { _, _, _ -> updateSpeedLabel() }
        binding.switchSchedule.isChecked = repo.scheduleEnabled
        binding.switchCatalog.isChecked = repo.catalogEnabled
        buildCatalogRows(repo.catalogFolderNames)
        binding.sliderErrorRetries.value = repo.errorRetries.toFloat()
        binding.switchPauseAfterError.isChecked = repo.pauseAfterError
        binding.switchAutoResume.isChecked = repo.autoResume
        startMinute = repo.scheduleStartMinute
        stopMinute = repo.scheduleStopMinute
        updateScheduleLabels()
        binding.btnScheduleStart.setOnClickListener { pickTime(startMinute) { startMinute = it; updateScheduleLabels() } }
        binding.btnScheduleStop.setOnClickListener { pickTime(stopMinute) { stopMinute = it; updateScheduleLabels() } }
        binding.switchSoundComplete.isChecked = repo.soundOnComplete
        binding.switchVibrateComplete.isChecked = repo.vibrateOnComplete
        binding.switchNotifLockScreen.isChecked = repo.notifOnLockScreen
        binding.switchNotifPauseAll.isChecked = repo.notifPauseAllButton
        binding.switchKeepScreenOn.isChecked = repo.keepScreenOn
        binding.switchClearFinished.isChecked = repo.clearFinishedOnStart
        binding.switchTorrentSequential.isChecked = repo.torrentSequential
        binding.switchTorrentSeed.isChecked = repo.torrentSeedAfterFinish
        binding.sliderSeedRatio.value = repo.seedRatioTenths.toFloat()
        binding.sliderSeedMinutes.value = repo.seedMinutes.toFloat()
        binding.switchTorrentDht.isChecked = repo.torrentDht
        binding.switchBottomBar.isChecked = repo.bottomBar
        binding.switchBottomBar.setOnCheckedChangeListener { _, on -> repo.bottomBar = on }
        val themeIds = listOf(khaled.benzo.R.id.themeDevice, khaled.benzo.R.id.themeGreen, khaled.benzo.R.id.themeBlue, khaled.benzo.R.id.themePurple, khaled.benzo.R.id.themeOrange)
        binding.radioThemeColor.check(themeIds[repo.themeColor.coerceIn(0, 4)])
        binding.radioThemeColor.setOnCheckedChangeListener { _, id ->
            val index = themeIds.indexOf(id)
            if (index >= 0 && index != repo.themeColor) { repo.themeColor = index; recreate() }
        }
        binding.sliderTorrentConnections.value = repo.torrentConnections.toFloat()
        binding.switchNewestFirst.isChecked = repo.newestFirst
        binding.sliderMinThread.value = repo.minThreadKb.toFloat()
        binding.sliderVibSuccess.value = repo.vibrateSuccessMs.toFloat()
        binding.sliderVibError.value = repo.vibrateErrorMs.toFloat()
        binding.switchVibrateError.isChecked = repo.vibrateOnError
        showUserAgentRow(repo.defaultUserAgent)
        binding.switchTorrentFixedPort.isChecked = repo.torrentFixedPort
        binding.switchTorrentAnonymous.isChecked = repo.torrentAnonymous
        binding.editTorrentPort.setText(repo.torrentPort.toString())
        binding.editTorrentPort.isEnabled = repo.torrentFixedPort
        binding.switchTorrentFixedPort.setOnCheckedChangeListener { _, on -> binding.editTorrentPort.isEnabled = on }
        updateConnectionsLabel()
        binding.sliderTorrentConnections.addOnChangeListener { _, _, _ -> updateConnectionsLabel() }
        updateSeedLabels()
        binding.sliderSeedRatio.addOnChangeListener { _, _, _ -> updateSeedLabels() }
        binding.sliderBattery.addOnChangeListener { _, _, _ -> updateBatteryLabel() }
        updateBatteryLabel()
        binding.sliderSeedMinutes.addOnChangeListener { _, _, _ -> updateSeedLabels() }
        binding.switchBrowserHistory.isChecked = repo.browserHistory
        binding.switchBrowserSaveTabs.isChecked = repo.browserSaveTabs
    }

    private fun saveAndFinish() {
        val repo = khaled.benzo.data.repository.SettingsRepository(this)
        repo.newestFirst = binding.switchNewestFirst.isChecked
        repo.minThreadKb = binding.sliderMinThread.value.toInt()
        repo.vibrateSuccessMs = binding.sliderVibSuccess.value.toInt()
        repo.vibrateErrorMs = binding.sliderVibError.value.toInt()
        repo.vibrateOnError = binding.switchVibrateError.isChecked
        repo.defaultUserAgent = userAgentValue
        repo.torrentFixedPort = binding.switchTorrentFixedPort.isChecked
        repo.torrentAnonymous = binding.switchTorrentAnonymous.isChecked
        // An empty or out-of-range port keeps the previous saved port.
        binding.editTorrentPort.text?.toString()?.toIntOrNull()
            ?.takeIf { it in khaled.benzo.core.torrent.TorrentNetworkPolicy.MIN_PORT..khaled.benzo.core.torrent.TorrentNetworkPolicy.MAX_PORT }
            ?.let { repo.torrentPort = it }
        viewModel.saveSettings(
            maxDownloads = binding.sliderMaxDownloads.value.toInt(),
            defaultThreads = binding.sliderDefaultThreads.value.toInt(),
            wifiOnly = binding.switchWifiOnly.isChecked,
            batteryThreshold = binding.sliderBattery.value.toInt(),
            stopOnUnplug = binding.switchStopOnUnplug.isChecked,
            soundComplete = binding.switchSoundComplete.isChecked,
            vibrateComplete = binding.switchVibrateComplete.isChecked,
            speedLimitKBps = binding.sliderSpeedLimit.value.toInt(),
            scheduleEnabled = binding.switchSchedule.isChecked,
            scheduleStartMinute = startMinute,
            scheduleStopMinute = stopMinute,
            catalogEnabled = binding.switchCatalog.isChecked,
            errorRetries = binding.sliderErrorRetries.value.toInt(),
            pauseAfterError = binding.switchPauseAfterError.isChecked,
            autoResume = binding.switchAutoResume.isChecked,
            notifLockScreen = binding.switchNotifLockScreen.isChecked,
            notifPauseAll = binding.switchNotifPauseAll.isChecked,
            keepScreenOn = binding.switchKeepScreenOn.isChecked,
            clearFinished = binding.switchClearFinished.isChecked,
            torrentSequential = binding.switchTorrentSequential.isChecked,
            torrentSeed = binding.switchTorrentSeed.isChecked,
            seedRatioTenths = binding.sliderSeedRatio.value.toInt(),
            seedMinutes = binding.sliderSeedMinutes.value.toInt(),
            torrentDht = binding.switchTorrentDht.isChecked,
            torrentConnections = binding.sliderTorrentConnections.value.toInt(),
            browserHistory = binding.switchBrowserHistory.isChecked,
            browserSaveTabs = binding.switchBrowserSaveTabs.isChecked
        )
        finish()
    }

    override fun onResume() {
        super.onResume()
        buildCatalogRows(khaled.benzo.data.repository.SettingsRepository(this).catalogFolderNames)
        refreshAllFilesButton()
    }

    /** Optional all-files access (Android 11+). Shows the current state; the user decides on the system screen. */
    private fun refreshAllFilesButton() {
        val b = binding.btnAllFilesAccess
        if (!khaled.benzo.ui.common.AllFilesAccess.isApplicable()) { b.visibility = android.view.View.GONE; return }
        b.visibility = android.view.View.VISIBLE
        val granted = khaled.benzo.ui.common.AllFilesAccess.isGranted()
        b.text = getString(if (granted) khaled.benzo.R.string.allfiles_state_on else khaled.benzo.R.string.allfiles_state_off)
        b.setOnClickListener { khaled.benzo.ui.common.AllFilesAccess.explainAndRequest(this) }
    }

    override fun onBackPressed() {
        saveAndFinish()
    }
}
