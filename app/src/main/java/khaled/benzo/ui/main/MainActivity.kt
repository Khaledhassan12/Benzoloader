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

    private fun setupNavigationDrawer() {
        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.nav_all,
            R.string.nav_all
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

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
                R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
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

    private fun setupListeners() {
        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, EditorActivity::class.java))
        }

        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun observeViewModel() {
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
                    viewModel.deleteDownload(item.id)
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
            R.id.sort_by_name -> { viewModel.setSort(khaled.benzo.core.queue.SortKey.NAME); true }
            R.id.sort_by_size -> { viewModel.setSort(khaled.benzo.core.queue.SortKey.SIZE); true }
            R.id.sort_by_date -> { viewModel.setSort(khaled.benzo.core.queue.SortKey.TIME); true }
            R.id.sort_by_type -> { viewModel.setSort(khaled.benzo.core.queue.SortKey.CATEGORY); true }
            R.id.action_export_list -> { listFiles.exportList(); true }
            R.id.action_import_list -> { listFiles.importList(); true }
            R.id.action_browser -> {
                startActivity(Intent(this, BrowserActivity::class.java))
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
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
