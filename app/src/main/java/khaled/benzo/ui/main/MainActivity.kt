package khaled.benzo.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import khaled.benzo.R
import khaled.benzo.data.model.DownloadItem
import khaled.benzo.data.model.DownloadStatus
import khaled.benzo.data.model.FileCategory
import khaled.benzo.databinding.ActivityMainBinding
import khaled.benzo.receiver.DownloadActionReceiver
import khaled.benzo.service.DownloadService
import khaled.benzo.ui.browser.BrowserActivity
import khaled.benzo.ui.editor.EditorActivity
import khaled.benzo.ui.props.FilePropertiesActivity
import khaled.benzo.ui.settings.SettingsActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var adapter: DownloadAdapter
    private val notificationPermission = NotificationPermissionFlow(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        setupNavigationDrawer()
        setupFilterPanel()
        setupQuickPanel()
        setupRecyclerView()
        setupListeners()
        observeViewModel()
        if (savedInstanceState == null) {
            notificationPermission.start()
            clearFinishedIfEnabled()
        }
    }

    override fun onResume() {
        super.onResume()
        applyBottomBar()
        applySideMenuSwap()
        viewModel.setAddToBottom(khaled.benzo.data.repository.SettingsRepository(this).listAddToBottom)
        // Applied on every return to the screen so a change in Settings takes effect without a restart.
        if (khaled.benzo.data.repository.SettingsRepository(this).keepScreenOn) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    /** "Clear finished downloads": removes finished list entries at app start. Files stay on disk. */
    private fun clearFinishedIfEnabled() {
        val enabled = khaled.benzo.data.repository.SettingsRepository(this).clearFinishedOnStart
        if (!enabled) return
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val repo = khaled.benzo.data.repository.DownloadRepository(applicationContext)
            khaled.benzo.core.housekeeping.ClearFinishedPolicy.idsToClear(repo.getAllOnce(), true).forEach { repo.deleteDownload(it) }
        }
    }

    private lateinit var filterPanel: FilterPanel

    private fun categoryTitle(c: FileCategory): String = getString(when (c) {
        FileCategory.OTHER -> R.string.nav_other
        FileCategory.ARCHIVE -> R.string.nav_archives
        FileCategory.PROGRAM -> R.string.nav_programs
        FileCategory.VIDEO -> R.string.nav_video
        FileCategory.MUSIC -> R.string.nav_music
        FileCategory.IMAGE -> R.string.nav_images
        FileCategory.DOCUMENT -> R.string.nav_documents
        FileCategory.TORRENT -> R.string.nav_torrent
        else -> R.string.panel_all
    })

    private lateinit var quickPanel: QuickPanel

    /** The left menu of the reference is the quick-settings panel: graph, bar, sections, Exit/Settings/Browser. It replaces the old category menu (categories live in the filter panel). */
    private fun setupQuickPanel() {
        val settings = khaled.benzo.data.repository.SettingsRepository(this)
        quickPanel = QuickPanel(
            this, settings,
            onOpenSettings = { startActivity(Intent(this, khaled.benzo.ui.settings.SettingsHubActivity::class.java)) },
            onOpenBrowser = { startActivity(Intent(this, BrowserActivity::class.java)) },
            onOpenSiteManager = { startActivity(Intent(this, khaled.benzo.ui.site.SiteManagerActivity::class.java)) },
            onExportList = { showExportDialog() },
            onImportList = { listFiles.importList() },
            onOpenBatch = { startActivity(Intent(this, khaled.benzo.ui.batch.BatchActivity::class.java)) },
            onOpenTopWindow = { startActivity(Intent(this, khaled.benzo.ui.settings.TopWindowSettingsActivity::class.java)) },
            onExit = {
                startService(Intent(this, DownloadService::class.java).apply { action = khaled.benzo.receiver.DownloadActionReceiver.ACTION_EXIT_APP })
                finishAffinity()
            },
            onChanged = { startService(Intent(this, DownloadService::class.java).apply { action = DownloadService.ACTION_PROCESS_QUEUE }) }
        )
        binding.navigationView.addView(quickPanel, android.widget.FrameLayout.LayoutParams(-1, -1))
        binding.drawerLayout.addDrawerListener(object : androidx.drawerlayout.widget.DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) { if (drawerView === binding.navigationView) quickPanel.refresh() }
        })
        lifecycleScope.launch { viewModel.allItems.collect { quickPanel.update(it) } }
    }

    /** Builds the side panel and keeps it, the toolbar subtitle and the list in step with the list state. */
    private fun setupFilterPanel() {
        filterPanel = FilterPanel(
            this,
            onType = { viewModel.setTypeOnly(it) },
            onStatus = { viewModel.setStatusOnly(it) },
            onTime = { viewModel.setTime(it) },
            onSort = { key, asc -> viewModel.setSortDirection(key, asc) },
            onClear = { viewModel.clearFilters() }
        )
        binding.filterPanelHost.addView(filterPanel)
        lifecycleScope.launch {
            kotlinx.coroutines.flow.combine(viewModel.listState, viewModel.allItems) { s, all -> s to all }.collect { (s, all) ->
                filterPanel.render(s, all)
                // Observed: the title is "All" or the chosen category; marks follow it for a custom sort or a time filter.
                // The marks are text stand-ins; the reference uses small icons.
                val base = s.type?.let { categoryTitle(it) } ?: getString(R.string.panel_all)
                supportActionBar?.title = base + (if (s.sortIsCustom) " \u21C5" else "") + (if (s.timeIsCustom) " \u25F7" else "")
                supportActionBar?.subtitle = when (s.status) {
                    khaled.benzo.core.queue.StatusFilter.ALL -> null
                    khaled.benzo.core.queue.StatusFilter.PAUSED -> getString(R.string.panel_stopped)
                    khaled.benzo.core.queue.StatusFilter.ACTIVE -> getString(R.string.panel_running)
                    khaled.benzo.core.queue.StatusFilter.FAILED -> getString(R.string.nav_failed)
                    khaled.benzo.core.queue.StatusFilter.COMPLETED -> getString(R.string.panel_successful)
                    khaled.benzo.core.queue.StatusFilter.QUEUED -> getString(R.string.nav_queued)
                    khaled.benzo.core.queue.StatusFilter.UNFINISHED -> getString(R.string.panel_unfinished)
                }
            }
        }
    }

    /** Edge of the navigation drawer; the filter panel takes the other one. Re-read on every return to the screen. */
    private var navGravity = GravityCompat.START
    private var panelGravity = GravityCompat.END

    private fun applySideMenuSwap() {
        val swap = khaled.benzo.data.repository.SettingsRepository(this).swapSideMenus
        val nav = if (swap) GravityCompat.END else GravityCompat.START
        val panel = if (swap) GravityCompat.START else GravityCompat.END
        if (nav == navGravity && binding.navigationView.layoutParams.let { (it as androidx.drawerlayout.widget.DrawerLayout.LayoutParams).gravity == nav }) return
        binding.drawerLayout.closeDrawers()
        navGravity = nav
        panelGravity = panel
        (binding.navigationView.layoutParams as androidx.drawerlayout.widget.DrawerLayout.LayoutParams).gravity = nav
        (binding.filterPanelScroll.layoutParams as androidx.drawerlayout.widget.DrawerLayout.LayoutParams).gravity = panel
        binding.navigationView.requestLayout()
        binding.filterPanelScroll.requestLayout()
    }

    private fun setupNavigationDrawer() {
        applySideMenuSwap()
        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.nav_all,
            R.string.nav_all
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        // The toolbar button opens the navigation drawer on whichever edge it sits.
        binding.toolbar.setNavigationOnClickListener { binding.drawerLayout.openDrawer(navGravity) }

        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_all -> viewModel.setCategory(FileCategory.ALL)
                R.id.nav_downloading -> viewModel.setCategory(FileCategory.DOWNLOADING)
                R.id.nav_completed -> viewModel.setCategory(FileCategory.COMPLETED)
                R.id.nav_queued -> viewModel.setCategory(FileCategory.QUEUED)
                R.id.nav_paused -> viewModel.setStatusFilter(khaled.benzo.core.queue.StatusFilter.PAUSED)
                R.id.nav_failed -> viewModel.setStatusFilter(khaled.benzo.core.queue.StatusFilter.FAILED)
                R.id.nav_video -> viewModel.setCategory(FileCategory.VIDEO)
                R.id.nav_music -> viewModel.setCategory(FileCategory.AUDIO)
                R.id.nav_documents -> viewModel.setCategory(FileCategory.DOCUMENTS)
                R.id.nav_archives -> viewModel.setCategory(FileCategory.ARCHIVES)
                R.id.nav_programs -> viewModel.setCategory(FileCategory.PROGRAMS)
                R.id.nav_images -> viewModel.setCategory(FileCategory.IMAGE)
                R.id.nav_other -> viewModel.setCategory(FileCategory.OTHER)
                R.id.nav_torrent -> viewModel.setCategory(FileCategory.TORRENT)
                R.id.nav_browser -> startActivity(Intent(this, BrowserActivity::class.java))
                R.id.nav_settings -> startActivity(Intent(this, khaled.benzo.ui.settings.SettingsHubActivity::class.java))
            }
            binding.drawerLayout.closeDrawer(navGravity)
            true
        }
    }

    private fun setupRecyclerView() {
        adapter = DownloadAdapter(
            onItemLongClick = { item -> selection = selection.toggle(item.id); onSelectionChanged() },
            onItemClick = { item ->
                if (selection.active) { selection = selection.toggle(item.id); onSelectionChanged(); return@DownloadAdapter }
                if (item.needsReentry) { showReentry(item); return@DownloadAdapter }
                val intent = Intent(this, FilePropertiesActivity::class.java).apply {
                    putExtra("download_id", item.id)
                }
                startActivity(intent)
            },
            onControlClick = { item ->
                toggleDownloadStatus(item)
            },
            onMenuClick = { item, view ->
                showItemPopupMenu(item, view)
            }
        )

        binding.recyclerViewDownloads.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewDownloads.adapter = adapter
    }

    // ---- Selection mode (video: back arrow, "n / total", delete, info, overflow) ----
    private var selection = khaled.benzo.core.queue.SelectionState()
    private var actionMode: androidx.appcompat.view.ActionMode? = null

    private fun visibleIds() = adapter.currentList.map { it.id }

    private fun onSelectionChanged() {
        adapter.setSelected(selection.selected)
        if (!selection.active) { actionMode?.finish(); return }
        if (actionMode == null) actionMode = startSupportActionMode(selectionCallback)
        val total = adapter.currentList.size
        actionMode?.title = "${selection.count} / $total"
        val bytes = adapter.currentList.filter { it.id in selection.selected && it.totalBytes > 0 }.sumOf { it.totalBytes }
        actionMode?.subtitle = if (bytes > 0) khaled.benzo.core.download.SpeedTracker.formatSize(bytes) else null
        actionMode?.invalidate()
    }

    private val selectionCallback = object : androidx.appcompat.view.ActionMode.Callback {
        override fun onCreateActionMode(mode: androidx.appcompat.view.ActionMode, menu: android.view.Menu): Boolean {
            menu.add(0, 1, 0, R.string.sel_delete).setIcon(R.drawable.ic_delete).setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS)
            menu.add(0, 2, 1, R.string.sel_info).setIcon(R.drawable.ic_info).setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS)
            menu.add(0, 7, 1, R.string.sel_edit).setIcon(R.drawable.ic_edit).setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS)
            menu.add(0, 8, 2, R.string.sel_send); menu.add(0, 9, 2, R.string.sel_open)
            menu.add(0, 3, 2, R.string.sel_start); menu.add(0, 4, 3, R.string.sel_stop)
            menu.add(0, 10, 3, R.string.sel_force_recheck); menu.add(0, 11, 3, R.string.sel_force_announce)
            menu.add(0, 12, 5, R.string.sel_to_top); menu.add(0, 13, 5, R.string.sel_to_bottom)
            menu.add(0, 14, 5, R.string.sel_up); menu.add(0, 15, 5, R.string.sel_down)
            menu.add(0, 5, 4, R.string.sel_select_all); menu.add(0, 6, 5, R.string.sel_invert)
            return true
        }
        override fun onPrepareActionMode(mode: androidx.appcompat.view.ActionMode, menu: android.view.Menu): Boolean {
            menu.findItem(2)?.isVisible = selection.count == 1
            menu.findItem(7)?.isVisible = selection.count == 1
            val tor = khaled.benzo.core.torrent.TorrentActionPolicy.showActions(adapter.currentList.filter { it.id in selection.selected })
            menu.findItem(10)?.isVisible = tor; menu.findItem(11)?.isVisible = tor   // Properties shows one item
            return true
        }
        override fun onActionItemClicked(mode: androidx.appcompat.view.ActionMode, item: android.view.MenuItem): Boolean {
            val chosen = adapter.currentList.filter { it.id in selection.selected }
            when (item.itemId) {
                1 -> androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle(R.string.sel_delete_title).setMessage(getString(R.string.sel_delete_msg, chosen.size))
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        // Same path as the row menu: through the service, so a running transfer stops first.
                        chosen.forEach { d -> startService(Intent(this@MainActivity, DownloadService::class.java).apply {
                            action = DownloadActionReceiver.ACTION_CANCEL; putExtra(DownloadActionReceiver.EXTRA_DOWNLOAD_ID, d.id) }) }
                        mode.finish()
                    }.setNegativeButton(android.R.string.cancel, null).show()
                2 -> chosen.firstOrNull()?.let { startActivity(Intent(this@MainActivity, FilePropertiesActivity::class.java).putExtra("download_id", it.id)) }
                8 -> showSendChoice(chosen)
                9 -> {
                    val one = khaled.benzo.core.files.FileActionPolicy.openTarget(chosen) { d -> khaled.benzo.ui.main.ItemFileIntents.file(d).isFile }
                    if (one == null) android.widget.Toast.makeText(this@MainActivity, R.string.sel_open_one, android.widget.Toast.LENGTH_LONG).show()
                    else try { startActivity(khaled.benzo.ui.main.ItemFileIntents.open(this@MainActivity, one)) }
                        catch (e: android.content.ActivityNotFoundException) { android.widget.Toast.makeText(this@MainActivity, R.string.sel_no_app, android.widget.Toast.LENGTH_LONG).show() }
                }
                10, 11 -> {
                    val targets = khaled.benzo.core.torrent.TorrentActionPolicy.targets(chosen)
                    val engine = khaled.benzo.core.torrent.TorrentEngine.orNull()
                    // Only torrents the engine has loaded can act; the others are named in the message, never reported as done.
                    val done = if (engine == null) 0 else targets.count { t ->
                        val h = t.torrentInfoHash ?: return@count false
                        if (item.itemId == 10) engine.forceRecheck(h) else engine.forceReannounce(h)
                    }
                    val msg = if (done == targets.size) getString(R.string.sel_torrent_done, done)
                              else getString(R.string.sel_torrent_partial, done, targets.size)
                    android.widget.Toast.makeText(this@MainActivity, msg, android.widget.Toast.LENGTH_LONG).show()
                }
                12, 13, 14, 15 -> {
                    if (!viewModel.isOrderSort()) {
                        // Hand moves are stored in the Order sort. Switching the list order is the user's decision, so ask.
                        androidx.appcompat.app.AlertDialog.Builder(this@MainActivity).setMessage(R.string.sel_move_needs_order)
                            .setPositiveButton(android.R.string.ok) { _, _ -> viewModel.setOrderSort() }
                            .setNegativeButton(android.R.string.cancel, null).show()
                    } else viewModel.moveItems(adapter.currentList, selection.selected, when (item.itemId) {
                        12 -> khaled.benzo.core.queue.OrderMoves.Move.TOP; 13 -> khaled.benzo.core.queue.OrderMoves.Move.BOTTOM
                        14 -> khaled.benzo.core.queue.OrderMoves.Move.UP; else -> khaled.benzo.core.queue.OrderMoves.Move.DOWN })
                }
                7 -> chosen.firstOrNull()?.let { startActivity(Intent(this@MainActivity, khaled.benzo.ui.editor.EditorActivity::class.java).putExtra(khaled.benzo.ui.editor.EditorActivity.EXTRA_EDIT_ID, it.id)); mode.finish() }
                3 -> chosen.filter { it.status != DownloadStatus.RUNNING && it.status != DownloadStatus.COMPLETED }.forEach { toggleDownloadStatus(it) }
                4 -> chosen.filter { it.status == DownloadStatus.RUNNING }.forEach { toggleDownloadStatus(it) }
                5 -> { selection = selection.selectAll(visibleIds()); onSelectionChanged() }
                6 -> { selection = selection.invert(visibleIds()); onSelectionChanged() }
            }
            return true
        }
        override fun onDestroyActionMode(mode: androidx.appcompat.view.ActionMode) {
            actionMode = null; selection = selection.clear(); adapter.setSelected(emptySet())
        }
    }

    /**
     * Send asks what to send, because the reference did not show it (UNCLEAR). Nothing is sent without the system chooser
     * and the user's tap there. A link is shown in full first, with a warning if it may hold a private key.
     */
    private fun showSendChoice(chosen: List<DownloadItem>) {
        val files = khaled.benzo.core.files.FileActionPolicy.usable(chosen) { d -> khaled.benzo.ui.main.ItemFileIntents.file(d).isFile }
        val links = khaled.benzo.core.files.SendLinkPolicy.links(chosen)
        if (files.isEmpty() && links.isEmpty()) { android.widget.Toast.makeText(this, R.string.sel_no_file, android.widget.Toast.LENGTH_LONG).show(); return }
        val labels = mutableListOf<String>(); val actions = mutableListOf<() -> Unit>()
        if (files.isNotEmpty()) { labels += getString(R.string.send_as_file, files.size); actions += { startActivity(khaled.benzo.ui.main.ItemFileIntents.send(this, files)) } }
        if (links.isNotEmpty()) { labels += getString(R.string.send_as_link, links.size); actions += { showSendLinkPreview(links) } }
        androidx.appcompat.app.AlertDialog.Builder(this).setTitle(R.string.sel_send)
            .setItems(labels.toTypedArray()) { _, i -> actions[i]() }
            .setNegativeButton(android.R.string.cancel, null).show()
    }

    private fun showSendLinkPreview(links: List<String>) {
        val warn = khaled.benzo.core.files.SendLinkPolicy.anyMayBePrivate(links)
        val body = (if (warn) getString(R.string.send_link_warning) + "\n\n" else "") + links.joinToString("\n")
        androidx.appcompat.app.AlertDialog.Builder(this).setTitle(R.string.send_link_title).setMessage(body)
            .setPositiveButton(R.string.sel_send) { _, _ -> startActivity(khaled.benzo.ui.main.ItemFileIntents.sendLinks(links)) }
            .setNegativeButton(android.R.string.cancel, null).show()
    }

    private fun showReentry(item: DownloadItem) {
        CredentialReentryDialog.show(
            this, askCookies = item.rawCookies != null, askHeaders = item.rawHeaders != null,
            onSave = { c, h -> viewModel.repairCredentials(item.id, c, h, false, ::reentryFailed) },
            onDiscard = { viewModel.repairCredentials(item.id, null, null, true, ::reentryFailed) }
        )
    }

    private fun reentryFailed() =
        android.widget.Toast.makeText(this, R.string.secrets_save_failed, android.widget.Toast.LENGTH_LONG).show()

    /** Bottom bar is shown or hidden from Settings > Interface. When it is shown its add button replaces the floating one. */
    private fun applyBottomBar() {
        val show = khaled.benzo.data.repository.SettingsRepository(this).bottomBar
        binding.bottomBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.fabAdd.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun setupBottomBar() {
        binding.navAdd.setOnClickListener { binding.fabAdd.performClick() }
        binding.navSettings.setOnClickListener { startActivity(Intent(this, khaled.benzo.ui.settings.SettingsHubActivity::class.java)) }
        binding.navBrowser.setOnClickListener { startActivity(Intent(this, BrowserActivity::class.java)) }
        binding.navQueue.setOnClickListener { viewModel.setStatusFilter(khaled.benzo.core.queue.StatusFilter.UNFINISHED) }
        binding.navFinished.setOnClickListener { viewModel.setCategory(FileCategory.COMPLETED) }
        binding.navList.setOnClickListener { binding.drawerLayout.openDrawer(navGravity) }
        binding.navPower.setOnClickListener { v ->
            PopupMenu(this, v).apply {
                menu.add(0, 1, 0, R.string.action_resume_all)
                menu.add(0, 2, 1, R.string.action_pause_all)
                setOnMenuItemClickListener {
                    if (it.itemId == 1) {
                        viewModel.resumeAll()
                        startService(Intent(this@MainActivity, DownloadService::class.java).apply { action = DownloadService.ACTION_PROCESS_QUEUE })
                    } else viewModel.pauseAll()
                    true
                }
                show()
            }
        }
    }

    private fun setupListeners() {
        setupBottomBar()
        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, EditorActivity::class.java))
        }

        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.counts.collect { (open, done) ->
                binding.badgeQueue.text = open.toString(); binding.badgeQueue.visibility = if (open > 0) View.VISIBLE else View.GONE
                binding.badgeFinished.text = done.toString(); binding.badgeFinished.visibility = if (done > 0) View.VISIBLE else View.GONE
            }
        }
        lifecycleScope.launch {
            khaled.benzo.service.QueueWaitState.reasons.collect { adapter.setWaiting(it) }
        }
        lifecycleScope.launch {
            viewModel.downloads.collectLatest { list ->
                adapter.submitList(list)
                if (selection.active) { selection = selection.prune(list.map { it.id }); onSelectionChanged() }
                binding.layoutEmptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun toggleDownloadStatus(item: DownloadItem) {
        val serviceIntent = Intent(this, DownloadService::class.java).apply {
            putExtra(DownloadActionReceiver.EXTRA_DOWNLOAD_ID, item.id)
            action = if (item.status == DownloadStatus.RUNNING) {
                DownloadActionReceiver.ACTION_PAUSE
            } else {
                DownloadService.ACTION_START_DOWNLOAD
            }
        }
        startService(serviceIntent)
    }

    private fun showItemPopupMenu(item: DownloadItem, view: View) {
        val popup = PopupMenu(this, view)
        popup.menu.add(0, 1, 0, "Resume")
        popup.menu.add(0, 2, 1, "Delete")
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                1 -> {
                    toggleDownloadStatus(item)
                    true
                }
                2 -> {
                    // Through the service: it stops a running transfer (and removes a torrent from the session) before the row goes.
                    startService(Intent(this, DownloadService::class.java).apply {
                        action = khaled.benzo.receiver.DownloadActionReceiver.ACTION_CANCEL
                        putExtra(khaled.benzo.receiver.DownloadActionReceiver.EXTRA_DOWNLOAD_ID, item.id)
                    })
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    override fun onStart() {
        super.onStart()
        // Opening the app is an allowed way to start the service: pick up queued downloads (for example after a reboot).
        khaled.benzo.service.NotificationHelper(this).cancelResumePrompt()
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val pending = khaled.benzo.data.db.BenzoDatabase.getInstance(this@MainActivity)
                .downloadDao().getPendingAndActiveDownloads().size
            if (pending > 0) {
                startService(Intent(this@MainActivity, DownloadService::class.java).apply {
                    action = DownloadService.ACTION_PROCESS_QUEUE
                })
            }
        }
    }

    private val listFiles = ListFileActions(this, { viewModel.exportText() }) { text, report ->
        importAny(text, report)
    }

    /** Four real entries (queue, finished, bookmarks, history). Site manager and authorization are NOT offered yet: see docs/EXPORT_SECRETS_DESIGN.md. */
    private fun showExportDialog() {
        val all = viewModel.allItems.value.filter { !it.isTorrent }
        val finished = all.count { it.status == khaled.benzo.data.model.DownloadStatus.COMPLETED }
        val counts = mapOf(khaled.benzo.core.data.ExportItem.QUEUE to all.size - finished, khaled.benzo.core.data.ExportItem.FINISHED to finished)
        val items = listOf(khaled.benzo.core.data.ExportItem.QUEUE, khaled.benzo.core.data.ExportItem.FINISHED, khaled.benzo.core.data.ExportItem.BOOKMARKS, khaled.benzo.core.data.ExportItem.HISTORY)
        val labels = arrayOf(getString(R.string.export_item_queue), getString(R.string.export_item_finished, finished), getString(R.string.export_item_bookmarks), getString(R.string.export_item_history))
        var sel = khaled.benzo.core.data.ExportSelection()
        val checked = BooleanArray(items.size)
        val dlg = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.export_dialog_title)
            .setMultiChoiceItems(labels, checked) { d, i, on ->
                checked[i] = on
                sel = sel.toggle(items[i])
                val ok = (d as androidx.appcompat.app.AlertDialog).getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                ok.isEnabled = sel.okEnabled
                val n = sel.okCount(counts)
                ok.text = if (n > 0) getString(R.string.export_ok_count, n) else getString(android.R.string.ok)
            }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val ticked = sel.writable()
                lifecycleScope.launch {
                    val parts = LinkedHashMap<String, String>()
                    val dl = viewModel.allItems.value
                    if (khaled.benzo.core.data.ExportItem.QUEUE in ticked) parts["queue"] = khaled.benzo.core.data.ListTransfer.format(dl.filter { it.status != khaled.benzo.data.model.DownloadStatus.COMPLETED })
                    if (khaled.benzo.core.data.ExportItem.FINISHED in ticked) parts["finished"] = viewModel.exportFinishedText()
                    if (khaled.benzo.core.data.ExportItem.BOOKMARKS in ticked) parts["bookmarks"] = viewModel.exportBookmarksText()
                    if (khaled.benzo.core.data.ExportItem.HISTORY in ticked) parts["history"] = viewModel.exportHistoryText()
                    listFiles.exportPrepared(khaled.benzo.core.data.ExportBundle.join(parts))
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
        dlg.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).isEnabled = false
    }

    /** Plain list files go to the download importer. Bundles: queue entries become paused downloads, finished entries are restored as completed records (no re-download, files untouched), bookmarks and history are added. */
    private fun importAny(text: String, report: (added: Int, skipped: Int) -> Unit) {
        val bundle = khaled.benzo.core.data.ExportBundle.split(text)
        if (bundle == null) { viewModel.importText(text, report); return }
        lifecycleScope.launch {
            var added = 0; var skipped = 0
            bundle["queue"]?.let { val (a, s) = viewModel.importDownloadsText(it); added += a; skipped += s }
            bundle["finished"]?.let { val (a, s) = viewModel.importFinishedText(it); added += a; skipped += s }
            bundle["history"]?.let { val (a, s) = viewModel.importHistoryText(it); added += a; skipped += s }
            bundle["bookmarks"]?.let { val (a, s) = viewModel.importBookmarksText(it); added += a; skipped += s }
            report(added, skipped)
        }
    }

    private val settingsFiles = SettingsFileActions(this, { khaled.benzo.data.repository.SettingsRepository(this) }) { recreate() }

    /** Settings only: downloads, history, bookmarks and saved logins stay as they are. */
    private fun confirmResetSettings() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setMessage(R.string.reset_settings_confirm)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                khaled.benzo.data.repository.SettingsRepository(this).resetToDefaults()
                khaled.benzo.service.ScheduleManager.rearm(this)
                android.widget.Toast.makeText(this, R.string.reset_settings_done, android.widget.Toast.LENGTH_LONG).show()
                lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    val pending = khaled.benzo.data.db.BenzoDatabase.getInstance(this@MainActivity).downloadDao().getPendingAndActiveDownloads().size
                    if (pending > 0) startService(Intent(this@MainActivity, DownloadService::class.java).apply { action = DownloadService.ACTION_PROCESS_QUEUE })
                }
                recreate()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val searchView = menu?.findItem(R.id.action_search)?.actionView as? androidx.appcompat.widget.SearchView
        searchView?.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setQuery(newText.orEmpty())
                return true
            }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_filter -> { binding.drawerLayout.openDrawer(panelGravity); true }
            R.id.sort_by_name -> { viewModel.setSort(khaled.benzo.core.queue.SortKey.NAME); true }
            R.id.sort_by_size -> { viewModel.setSort(khaled.benzo.core.queue.SortKey.SIZE); true }
            R.id.sort_by_date -> { viewModel.setSort(khaled.benzo.core.queue.SortKey.TIME); true }
            R.id.sort_by_type -> { viewModel.setSort(khaled.benzo.core.queue.SortKey.CATEGORY); true }
            R.id.action_export_list -> { showExportDialog(); true }
            R.id.action_import_list -> { listFiles.importList(); true }
            R.id.action_export_settings -> { settingsFiles.exportSettings(); true }
            R.id.action_import_settings -> { settingsFiles.importSettings(); true }
            R.id.action_reset_settings -> { confirmResetSettings(); true }
            R.id.action_browser -> {
                startActivity(Intent(this, BrowserActivity::class.java))
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, khaled.benzo.ui.settings.SettingsHubActivity::class.java))
                true
            }
            R.id.action_resume_all -> {
                viewModel.resumeAll()
                startService(Intent(this, DownloadService::class.java).apply {
                    action = DownloadService.ACTION_PROCESS_QUEUE
                })
                true
            }
            R.id.action_pause_all -> {
                viewModel.pauseAll()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
