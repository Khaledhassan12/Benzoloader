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
            onItemClick = { item ->
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
        viewModel.importText(text, report)
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
            R.id.action_export_list -> { listFiles.exportList(); true }
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
