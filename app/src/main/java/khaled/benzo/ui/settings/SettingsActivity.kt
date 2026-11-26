package khaled.benzo.ui.settings

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import khaled.benzo.R
import khaled.benzo.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private var encryptionIndex = 0

    private fun showEncryptionRow() {
        val v = when (khaled.benzo.core.torrent.TorrentEncryption.fromIndex(encryptionIndex)) {
            khaled.benzo.core.torrent.TorrentEncryption.PREFER -> R.string.enc_prefer
            khaled.benzo.core.torrent.TorrentEncryption.REQUIRE -> R.string.enc_require
            khaled.benzo.core.torrent.TorrentEncryption.DISABLE -> R.string.enc_disable
        }
        binding.txtTorrentEncryption.text = getString(R.string.pref_torrent_encryption) + ": " + getString(v)
    }

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
        bindDependents(binding.switchQuickAdd, binding.switchQuickAddStart)
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

    private fun proxyRepo() = khaled.benzo.data.repository.SettingsRepository(this)

    /** Title plus the chosen proxy (or "Disabled") in the accent colour; a tap opens the radio list. */
    private fun showProxyRow() {
        val row = binding.txtProxy
        val sel = proxyRepo().proxySelected
        val b = android.text.SpannableStringBuilder(getString(R.string.row_proxy)).append('\n')
        val start = b.length
        b.append(if (sel.isEmpty()) getString(R.string.proxy_disabled) else sel)
        b.setSpan(android.text.style.ForegroundColorSpan(com.google.android.material.color.MaterialColors.getColor(row, com.google.android.material.R.attr.colorPrimary)), start, b.length, 0)
        // Always visible while a proxy is chosen: which paths still connect directly.
        if (sel.isNotEmpty()) b.append('\n').append(getString(R.string.proxy_coverage))
        row.text = b
        val d = resources.displayMetrics.density
        row.minHeight = (72 * d).toInt(); row.gravity = android.view.Gravity.CENTER_VERTICAL
        row.setPadding((20 * d).toInt(), (12 * d).toInt(), (20 * d).toInt(), (12 * d).toInt())
        row.isClickable = true
        row.setOnClickListener { showProxyDialog() }
    }

    /** Radio list: Disabled first, then the entered proxies. New adds one (checked before saving), long-press removes one. Applied at once. */
    private fun showProxyDialog() {
        val repo = proxyRepo()
        val list = repo.proxyList
        val names = (listOf(getString(R.string.proxy_disabled)) + list).toTypedArray()
        var checked = if (repo.proxySelected.isEmpty()) 0 else (list.indexOf(repo.proxySelected) + 1).coerceAtLeast(0)
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.row_proxy)
            .setSingleChoiceItems(names, checked) { _, which -> checked = which }
            .setNeutralButton(R.string.proxy_new) { _, _ -> showProxyNew() }
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                repo.proxySelected = if (checked == 0) "" else list[checked - 1]
                showProxyRow()
                if (checked != 0) android.widget.Toast.makeText(this, R.string.proxy_scope_note, android.widget.Toast.LENGTH_LONG).show()
            }.create()
        dialog.show()
        dialog.listView.setOnItemLongClickListener { _, _, pos, _ ->
            if (pos == 0) return@setOnItemLongClickListener true
            val gone = list[pos - 1]
            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setMessage(getString(R.string.proxy_remove_msg, gone))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    repo.proxyList = list - gone
                    if (repo.proxySelected == gone) repo.proxySelected = ""
                    dialog.dismiss(); showProxyRow(); showProxyDialog()
                }.setNegativeButton(android.R.string.cancel, null).show()
            true
        }
    }

    private fun showProxyNew() {
        val edit = android.widget.EditText(this).apply { hint = getString(R.string.proxy_hint); setSingleLine() }
        val pad = (24 * resources.displayMetrics.density).toInt()
        val d = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.proxy_new)
            .setMessage(R.string.proxy_new_note)
            .setView(android.widget.FrameLayout(this).apply { setPadding(pad, 0, pad, 0); addView(edit) })
            .setNeutralButton(R.string.proxy_paste, null)
            .setNegativeButton(android.R.string.cancel) { _, _ -> showProxyDialog() }
            .setPositiveButton(android.R.string.ok, null).create()
        d.show()
        val ok = d.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
        ok.isEnabled = false
        edit.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { ok.isEnabled = !s.isNullOrBlank() }
        })
        d.getButton(android.app.AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            cm.primaryClip?.getItemAt(0)?.text?.toString()?.let { edit.setText(it.trim()) }
        }
        ok.setOnClickListener {
            when (val p = khaled.benzo.core.net.ProxyConfig.parse(edit.text.toString())) {
                is khaled.benzo.core.net.ProxyConfig.Parsed.Bad -> android.widget.Toast.makeText(this, R.string.proxy_invalid, android.widget.Toast.LENGTH_LONG).show()
                is khaled.benzo.core.net.ProxyConfig.Parsed.Ok -> {
                    val repo = proxyRepo(); repo.proxyList = repo.proxyList + p.config.label()
                    d.dismiss(); showProxyDialog()
                }
            }
        }
    }

    private var userAgentValue: String = ""

    /** Title plus the current value (or "Default") in the accent colour; tapping opens a text dialog. Saved with the rest on exit. */
    private fun showUserAgentRow(value: String) {
        userAgentValue = value
        val row = binding.txtUserAgent
        val title = getString(khaled.benzo.R.string.row_user_agent)
        val presetNames = intArrayOf(khaled.benzo.R.string.val_default, khaled.benzo.R.string.ua_mobile_chrome, khaled.benzo.R.string.ua_desktop_chrome, khaled.benzo.R.string.ua_desktop_firefox, khaled.benzo.R.string.ua_desktop_safari)
        val preset = khaled.benzo.core.settings.UserAgentPresets.indexOf(value)
        val shown = if (preset >= 0) getString(presetNames[preset]) else value
        val b = android.text.SpannableStringBuilder(title).append('\n')
        val start = b.length
        b.append(shown)
        b.setSpan(android.text.style.ForegroundColorSpan(com.google.android.material.color.MaterialColors.getColor(row, com.google.android.material.R.attr.colorPrimary)), start, b.length, 0)
        row.text = b
        val d = resources.displayMetrics.density
        row.minHeight = (72 * d).toInt(); row.gravity = android.view.Gravity.CENTER_VERTICAL
        row.setPadding((20 * d).toInt(), (12 * d).toInt(), (20 * d).toInt(), (12 * d).toInt())
        row.isClickable = true
        row.setOnClickListener { showUserAgentChoices(title) }
    }

    /** Preset list first (Default, four browser formats, Custom); Custom opens the text dialog. */
    private fun showUserAgentChoices(title: String) {
        val names = arrayOf(
            getString(khaled.benzo.R.string.val_default), getString(khaled.benzo.R.string.ua_mobile_chrome), getString(khaled.benzo.R.string.ua_desktop_chrome),
            getString(khaled.benzo.R.string.ua_desktop_firefox), getString(khaled.benzo.R.string.ua_desktop_safari), getString(khaled.benzo.R.string.ua_custom)
        )
        val current = khaled.benzo.core.settings.UserAgentPresets.indexOf(userAgentValue).let { if (it < 0) names.size - 1 else it }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setSingleChoiceItems(names, current) { dialog, which ->
                dialog.dismiss()
                if (which < names.size - 1) showUserAgentRow(khaled.benzo.core.settings.UserAgentPresets.values[which]) else showUserAgentText(title)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showUserAgentText(title: String) {
        val d = resources.displayMetrics.density
        run {
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

    /** Command buttons row: tap opens a checkbox list of seven entries. Android shows at most three, so a fourth tick is refused. Saved on each tick. */
    private fun setupCommandButtonsRow() {
        val repo = viewModel.repository
        val row = binding.txtCommandButtons
        val all = khaled.benzo.core.notify.NotifButton.values().toList()
        val labels = intArrayOf(khaled.benzo.R.string.cmd_start_all, khaled.benzo.R.string.cmd_stop_all, khaled.benzo.R.string.cmd_add, khaled.benzo.R.string.cmd_scheduler, khaled.benzo.R.string.cmd_settings, khaled.benzo.R.string.cmd_browser, khaled.benzo.R.string.cmd_exit)
        fun current() = khaled.benzo.core.notify.NotificationPolicy.commandButtons(repo.notifButtons, repo.notifPauseAllButton)
        fun show() {
            val names = current().joinToString(", ") { getString(labels[all.indexOf(it)]) }.ifEmpty { getString(khaled.benzo.R.string.cmd_none) }
            row.text = getString(khaled.benzo.R.string.row_command_buttons) + "\n" + names
        }
        row.setOnClickListener {
            var chosen = current()
            val checked = BooleanArray(all.size) { all[it] in chosen }
            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(khaled.benzo.R.string.row_command_buttons)
                .setMultiChoiceItems(Array(all.size) { getString(labels[it]) }, checked) { dlg, which, on ->
                    val next = khaled.benzo.core.notify.NotificationPolicy.toggled(chosen, all[which])
                    if (next == null) {
                        (dlg as androidx.appcompat.app.AlertDialog).listView.setItemChecked(which, false)
                        android.widget.Toast.makeText(this, khaled.benzo.R.string.cmd_max_three, android.widget.Toast.LENGTH_SHORT).show()
                    } else { chosen = next; repo.notifButtons = khaled.benzo.core.notify.NotificationPolicy.format(chosen); show() }
                }
                .setPositiveButton(android.R.string.ok, null).show()
        }
        show()
    }

    private var afterFinishCode = 0

    /** Value row for "after finishing": title, then the current choice in the accent colour; a tap opens a single-choice dialog. */
    private fun setupAfterFinishRow() {
        val row = binding.txtAfterFinish
        val d = resources.displayMetrics.density
        row.minHeight = (72 * d).toInt()
        row.gravity = android.view.Gravity.CENTER_VERTICAL
        row.setPadding((20 * d).toInt(), (12 * d).toInt(), (20 * d).toInt(), (12 * d).toInt())
        val tv = android.util.TypedValue(); theme.resolveAttribute(android.R.attr.selectableItemBackground, tv, true)
        row.setBackgroundResource(tv.resourceId)
        val labels = arrayOf(getString(khaled.benzo.R.string.after_finish_nothing), getString(khaled.benzo.R.string.after_finish_exit))
        fun show() {
            val accent = com.google.android.material.color.MaterialColors.getColor(row, com.google.android.material.R.attr.colorPrimary)
            val b = android.text.SpannableStringBuilder(getString(khaled.benzo.R.string.row_after_finish)).append('\n')
            val start = b.length
            b.append(labels[afterFinishCode])
            b.setSpan(android.text.style.ForegroundColorSpan(accent), start, b.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            row.text = b
        }
        row.setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(khaled.benzo.R.string.row_after_finish)
                .setSingleChoiceItems(labels, afterFinishCode) { dlg, which -> afterFinishCode = which; show(); dlg.dismiss() }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
        show()
    }

    private var valueRows: List<ValueRow> = emptyList()
    private fun refreshValueRows() = valueRows.forEach { it.refresh() }

    private fun setupValueRows() {
        val g = { id: Int -> getString(id) }
        valueRows = listOf(
            ValueRow(this, binding.txtMaxDownloads, binding.sliderMaxDownloads, g(R.string.row_max_downloads), { getString(R.string.val_count, it) }),
            ValueRow(this, binding.txtDefaultThreads, binding.sliderDefaultThreads, g(R.string.row_threads), { getString(R.string.val_threads, it) }),
            ValueRow(this, binding.txtMinThread, binding.sliderMinThread, g(R.string.row_min_thread), { if (it >= 1024) "%.1f MB".format(it / 1024f) else "$it KB" }),
            ValueRow(this, binding.txtVibSuccess, binding.sliderVibSuccess, g(R.string.row_vib_success), { getString(R.string.val_ms, it) }),
            ValueRow(this, binding.txtVibTotal, binding.sliderVibTotal, g(R.string.row_vib_total), { getString(R.string.val_ms, it) }),
            ValueRow(this, binding.txtVibError, binding.sliderVibError, g(R.string.row_vib_error), { getString(R.string.val_ms, it) }),
            ValueRow(this, binding.txtErrorRetries, binding.sliderErrorRetries, g(R.string.row_retries), { getString(R.string.val_count, it) }),
            ValueRow(this, binding.txtSpeedLimit, binding.sliderSpeedLimit, g(R.string.row_speed),
                { if (it == 0) g(R.string.pref_speed_unlimited) else getString(R.string.val_kbps, it) }),
            ValueRow(this, binding.txtBatteryThreshold, binding.sliderBattery, g(R.string.row_battery), { getString(R.string.val_percent, it) },
                { getString(R.string.pref_battery_threshold_fmt, it, khaled.benzo.core.queue.QueuePolicy.restartBelowOrAt(it) + 1) }),
            ValueRow(this, binding.txtSeedRatio, binding.sliderSeedRatio, g(R.string.row_seed_ratio),
                { if (it == 0) g(R.string.pref_unlimited) else "%d.%d".format(it / 10, it % 10) }),
            ValueRow(this, binding.txtSeedMinutes, binding.sliderSeedMinutes, g(R.string.row_seed_time),
                { if (it == 0) g(R.string.pref_unlimited) else "%d:%02d".format(it / 60, it % 60) }),
            ValueRow(this, binding.txtTorrentConnections, binding.sliderTorrentConnections, g(R.string.row_connections),
                { if (it == 0) g(R.string.pref_torrent_connections_default) else it.toString() })
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
        binding.switchCreateEmptyFile.isChecked = repo.createEmptyFile
        // Saved on the tap, not with the Save list: it takes effect for the next download the service starts.
        binding.switchCreateEmptyFile.setOnCheckedChangeListener { _, on -> repo.createEmptyFile = on }
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
        binding.switchSwapMenus.isChecked = repo.swapSideMenus
        binding.switchSwapMenus.setOnCheckedChangeListener { _, on -> repo.swapSideMenus = on }
        val modeIds = listOf(khaled.benzo.R.id.modeSystem, khaled.benzo.R.id.modeLight, khaled.benzo.R.id.modeDark)
        binding.radioThemeMode.check(modeIds[repo.themeMode])
        binding.radioThemeMode.setOnCheckedChangeListener { _, id ->
            val index = modeIds.indexOf(id)
            if (index >= 0 && index != repo.themeMode) {
                repo.themeMode = index
                khaled.benzo.ui.common.ThemeColors.applyNightMode(this)
            }
        }
        val themeIds = listOf(khaled.benzo.R.id.themeDevice, khaled.benzo.R.id.themeGreen, khaled.benzo.R.id.themeBlue, khaled.benzo.R.id.themePurple, khaled.benzo.R.id.themeOrange)
        binding.radioThemeColor.check(themeIds[repo.themeColor.coerceIn(0, 4)])
        binding.radioThemeColor.setOnCheckedChangeListener { _, id ->
            val index = themeIds.indexOf(id)
            if (index >= 0 && index != repo.themeColor) { repo.themeColor = index; recreate() }
        }
        binding.sliderTorrentConnections.value = repo.torrentConnections.toFloat()
        binding.switchNewestFirst.isChecked = repo.newestFirst
        binding.switchQuickAdd.isChecked = repo.quickAddLinks
        afterFinishCode = repo.afterFinish
        setupAfterFinishRow()
        setupCommandButtonsRow()
        binding.switchQuickAddStart.isChecked = repo.quickAddStart
        binding.switchListBottom.isChecked = repo.listAddToBottom
        binding.sliderMinThread.value = repo.minThreadKb.toFloat()
        binding.sliderVibSuccess.value = repo.vibrateSuccessMs.toFloat()
        binding.sliderVibError.value = repo.vibrateErrorMs.toFloat()
        binding.sliderVibTotal.value = repo.vibrateTotalMs.toFloat()
        binding.switchVibrateError.isChecked = repo.vibrateOnError
        binding.switchClearNotifsOnExit.isChecked = repo.clearNotifsOnExit
        showUserAgentRow(repo.defaultUserAgent)
        showProxyRow()
        binding.switchTorrentFixedPort.isChecked = repo.torrentFixedPort
        binding.switchTorrentAnonymous.isChecked = repo.torrentAnonymous
        encryptionIndex = repo.torrentEncryption
        showEncryptionRow()
        binding.txtTorrentEncryption.setOnClickListener {
            val names = arrayOf(getString(R.string.enc_prefer), getString(R.string.enc_require), getString(R.string.enc_disable))
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.pref_torrent_encryption)
                .setSingleChoiceItems(names, encryptionIndex) { d, which -> encryptionIndex = which; showEncryptionRow(); d.dismiss() }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
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
        repo.quickAddLinks = binding.switchQuickAdd.isChecked
        repo.afterFinish = afterFinishCode
        repo.quickAddStart = binding.switchQuickAddStart.isChecked
        repo.listAddToBottom = binding.switchListBottom.isChecked
        repo.minThreadKb = binding.sliderMinThread.value.toInt()
        repo.vibrateSuccessMs = binding.sliderVibSuccess.value.toInt()
        repo.vibrateErrorMs = binding.sliderVibError.value.toInt()
        repo.vibrateTotalMs = binding.sliderVibTotal.value.toInt()
        repo.vibrateOnError = binding.switchVibrateError.isChecked
        repo.clearNotifsOnExit = binding.switchClearNotifsOnExit.isChecked
        repo.defaultUserAgent = userAgentValue
        repo.torrentFixedPort = binding.switchTorrentFixedPort.isChecked
        repo.torrentAnonymous = binding.switchTorrentAnonymous.isChecked
        repo.torrentEncryption = encryptionIndex
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
        binding.btnInterfaceColors.setOnClickListener { startActivity(android.content.Intent(this, InterfaceColorsActivity::class.java)) }
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
