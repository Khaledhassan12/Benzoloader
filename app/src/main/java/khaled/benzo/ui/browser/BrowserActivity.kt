package khaled.benzo.ui.browser

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.viewModels
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import khaled.benzo.databinding.ActivityBrowserBinding
import khaled.benzo.ui.editor.EditorActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import khaled.benzo.R

class BrowserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBrowserBinding
    private val viewModel: BrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWebView()
        setupListeners()
        observeViewModel()

        val initialUrl = intent.getStringExtra("extra_url") ?: "https://www.google.com"
        loadUrl(initialUrl)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = binding.webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        // Pages from the web never need to read local files, and secure pages must not load insecure content silently.
        settings.allowFileAccess = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        settings.userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                binding.progressBarBrowser.visibility = View.VISIBLE
                if (url != null) {
                    binding.edtBrowserUrl.setText(url)
                }
                viewModel.clearDetectedMedia()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.progressBarBrowser.visibility = View.GONE
                val pageUrl = url
                val pageTitle = view?.title
                lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) { viewModel.recordVisit(pageTitle, pageUrl) }
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                if (request != null) {
                    val url = request.url.toString()
                    val headers = request.requestHeaders
                    viewModel.mediaSniffer.inspectUrl(url, headers)
                }
                return super.shouldInterceptRequest(view, request)
            }
        }

        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                binding.progressBarBrowser.progress = newProgress
            }
        }
    }

    private fun setupListeners() {
        binding.btnBrowserBack.setOnClickListener {
            if (binding.webView.canGoBack()) {
                binding.webView.goBack()
            }
        }

        binding.btnBrowserForward.setOnClickListener {
            if (binding.webView.canGoForward()) {
                binding.webView.goForward()
            }
        }

        binding.btnBrowserRefresh.setOnClickListener {
            binding.webView.reload()
        }

        binding.edtBrowserUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                val input = binding.edtBrowserUrl.text.toString().trim()
                loadUrl(input)
                true
            } else {
                false
            }
        }

        binding.btnBrowserMenu.setOnClickListener { showMenu(it) }

        binding.fabMediaSniffer.setOnClickListener {
            val list = viewModel.detectedMediaList.value
            if (list.isNotEmpty()) {
                val dialog = MediaSnifferDialog(list) { selected ->
                    val intent = Intent(this, EditorActivity::class.java).apply {
                        putExtra("extra_url", selected.url)
                    }
                    startActivity(intent)
                }
                dialog.show(supportFragmentManager, "MediaSnifferDialog")
            }
        }
    }

    private fun showMenu(anchor: View) {
        val menu = android.widget.PopupMenu(this, anchor)
        menu.menu.add(0, 1, 0, R.string.browser_add_bookmark)
        menu.menu.add(0, 2, 1, R.string.browser_bookmarks)
        menu.menu.add(0, 3, 2, R.string.browser_history)
        menu.setOnMenuItemClickListener {
            when (it.itemId) {
                1 -> addBookmark()
                2 -> showBookmarks()
                3 -> showHistory()
            }
            true
        }
        menu.show()
    }

    private fun addBookmark() {
        val url = binding.webView.url
        val title = binding.webView.title
        lifecycleScope.launch {
            val saved = withContext(kotlinx.coroutines.Dispatchers.IO) { viewModel.addBookmark(title, url) }
            Toast.makeText(this@BrowserActivity, if (saved) R.string.browser_bookmark_added else R.string.browser_bookmark_not_added, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showBookmarks() {
        lifecycleScope.launch {
            val list = withContext(kotlinx.coroutines.Dispatchers.IO) { viewModel.bookmarks() }
            if (list.isEmpty()) {
                Toast.makeText(this@BrowserActivity, R.string.browser_bookmarks_empty, Toast.LENGTH_SHORT).show()
                return@launch
            }
            val dialog = AlertDialog.Builder(this@BrowserActivity)
                .setTitle(R.string.browser_bookmarks)
                .setItems(list.map { it.title }.toTypedArray()) { _, i -> loadUrl(list[i].url) }
                .setNegativeButton(R.string.browser_close, null)
                .create()
            dialog.show()
            // A long press deletes one bookmark, after a confirmation.
            dialog.listView.setOnItemLongClickListener { _, _, i, _ ->
                AlertDialog.Builder(this@BrowserActivity)
                    .setMessage(getString(R.string.browser_bookmark_delete_confirm, list[i].title))
                    .setPositiveButton(R.string.browser_delete) { _, _ ->
                        lifecycleScope.launch {
                            withContext(kotlinx.coroutines.Dispatchers.IO) { viewModel.deleteBookmark(list[i].id) }
                            dialog.dismiss()
                        }
                    }
                    .setNegativeButton(R.string.browser_close, null)
                    .show()
                true
            }
        }
    }

    private fun showHistory() {
        lifecycleScope.launch {
            val list = withContext(kotlinx.coroutines.Dispatchers.IO) { viewModel.history() }
            if (list.isEmpty()) {
                val msg = if (viewModel.historyEnabled) R.string.browser_history_empty else R.string.browser_history_off
                Toast.makeText(this@BrowserActivity, msg, Toast.LENGTH_LONG).show()
                return@launch
            }
            AlertDialog.Builder(this@BrowserActivity)
                .setTitle(R.string.browser_history)
                .setItems(list.map { it.title }.toTypedArray()) { _, i -> loadUrl(list[i].url) }
                .setNeutralButton(R.string.browser_history_clear) { _, _ ->
                    lifecycleScope.launch { withContext(kotlinx.coroutines.Dispatchers.IO) { viewModel.clearHistory() } }
                }
                .setNegativeButton(R.string.browser_close, null)
                .show()
        }
    }

    private fun loadUrl(input: String) {
        val url = if (input.startsWith("http://") || input.startsWith("https://")) {
            input
        } else if (input.contains(".")) {
            "https://$input"
        } else {
            "https://www.google.com/search?q=" + Uri.encode(input)
        }
        binding.webView.loadUrl(url)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.detectedMediaList.collectLatest { list ->
                if (list.isNotEmpty()) {
                    binding.fabMediaSniffer.visibility = View.VISIBLE
                    binding.fabMediaSniffer.text = "${list.size}"
                } else {
                    binding.fabMediaSniffer.visibility = View.GONE
                }
            }
        }
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
