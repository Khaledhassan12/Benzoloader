package khaled.benzo.ui.site

import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import khaled.benzo.databinding.ActivitySiteManagerBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SiteManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySiteManagerBinding
    private val viewModel: SiteManagerViewModel by viewModels()
    private lateinit var adapter: SiteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate()
        binding = ActivitySiteManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbarSite.setNavigationOnClickListener { finish() }

        adapter = SiteAdapter(mutableListOf()) { site ->
            viewModel.deleteSite(site.domain)
        }
        binding.recyclerViewSites.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewSites.adapter = adapter

        binding.fabAddSite.setOnClickListener {
            showAddSiteDialog()
        }

        lifecycleScope.launch {
            viewModel.sites.collectLatest { list ->
                adapter.updateList(list)
            }
        }

        viewModel.loadSites()
    }

    private fun showAddSiteDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
        }

        val edtDomain = EditText(this).apply { hint = "Domain (e.g. example.com)" }
        val edtUser = EditText(this).apply { hint = "Username" }
        val edtPass = EditText(this).apply { hint = "Password" }

        container.addView(edtDomain)
        container.addView(edtUser)
        container.addView(edtPass)

        AlertDialog.Builder(this)
            .setTitle("Add Site Account")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val domain = edtDomain.text.toString().trim()
                val user = edtUser.text.toString().trim()
                val pass = edtPass.text.toString().trim()
                if (domain.isNotEmpty()) {
                    viewModel.addSite(domain, user, pass)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
