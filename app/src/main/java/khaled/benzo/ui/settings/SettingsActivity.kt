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

    private fun updateSeedLabels() {
        val tenths = binding.sliderSeedRatio.value.toInt()
        val mins = binding.sliderSeedMinutes.value.toInt()
        binding.txtSeedRatio.text = getString(khaled.benzo.R.string.pref_seed_ratio) + ": " +
            if (tenths == 0) getString(khaled.benzo.R.string.pref_unlimited) else "%d.%d".format(tenths / 10, tenths % 10)
        binding.txtSeedMinutes.text = getString(khaled.benzo.R.string.pref_seed_minutes) + ": " +
            if (mins == 0) getString(khaled.benzo.R.string.pref_unlimited) else "%d:%02d".format(mins / 60, mins % 60)
    }

    private fun updateSpeedLabel() {
        val v = binding.sliderSpeedLimit.value.toInt()
        binding.txtSpeedLimit.text = if (v == 0) getString(khaled.benzo.R.string.pref_speed_limit) + ": " +
            getString(khaled.benzo.R.string.pref_speed_unlimited)
        else getString(khaled.benzo.R.string.pref_speed_limit_value, v)
    }

    private fun pickTime(current: Int, onPicked: (Int) -> Unit) {
        android.app.TimePickerDialog(this, { _, h, m -> onPicked(h * 60 + m) },
            current / 60, current % 60, true).show()
    }

    private fun updateConnectionsLabel() {
        val v = binding.sliderTorrentConnections.value.toInt()
        binding.txtTorrentConnections.text = getString(R.string.pref_torrent_connections) + ": " +
            if (v == 0) getString(R.string.pref_torrent_connections_default) else v.toString()
    }

    private fun updateBatteryLabel() {
        val t = binding.sliderBattery.value.toInt()
        binding.txtBatteryThreshold.text = getString(R.string.pref_battery_threshold_fmt, t, khaled.benzo.core.queue.QueuePolicy.restartBelowOrAt(t) + 1)
    }

    private fun loadCurrentSettings() {
        val repo = viewModel.repository
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
