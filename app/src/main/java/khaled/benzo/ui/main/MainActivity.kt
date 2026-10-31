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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        setupNavigationDrawer()
        setupRecyclerView()
        setupListeners()
        observeViewModel()
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
                R.id.nav_video -> viewModel.setCategory(FileCategory.VIDEO)
                R.id.nav_music -> viewModel.setCategory(FileCategory.AUDIO)
                R.id.nav_documents -> viewModel.setCategory(FileCategory.DOCUMENTS)
                R.id.nav_archives -> viewModel.setCategory(FileCategory.ARCHIVES)
                R.id.nav_programs -> viewModel.setCategory(FileCategory.PROGRAMS)
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
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
