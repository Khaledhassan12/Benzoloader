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
        bindDependents(binding.switchCatalog, binding.btnFolderNames)
        bindDependents(binding.switchSchedule, binding.btnScheduleStart, binding.btnScheduleStop)
    }

    /** Rows that only matter while [parent] is on are faded and disabled when it is off. Their values are still saved. */
    private fun bindDependents(parent: android.widget.CompoundButton, vararg rows: android.view.View) {
        fun apply(on: Boolean) = rows.forEach { it.isEnabled = on; it.alpha = if (on) 1f else 0.38f }
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

    private var valueRows: List<ValueRow> = emptyList()
    private fun refreshValueRows() = valueRows.forEach { it.refresh() }

    private fun setupValueRows() {
        val g = { id: Int -> getString(id) }
        val R = khaled.benzo.R.string
        valueRows = listOf(
            ValueRow(this, binding.txtMaxDownloads, binding.sliderMaxDownloads, g(R.row_max_downloads), { getString(R.val_count, it) }),
            ValueRow(this, binding.txtDefaultThreads, binding.sliderDefaultThreads, g(R.row_threads), { getString(R.val_threads, it) }),
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
        binding.btnFolderNames.setOnClickListener {
            FolderNamesDialog.show(this, repo.catalogFolderNames) { repo.catalogFolderNames = it }
        }
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
