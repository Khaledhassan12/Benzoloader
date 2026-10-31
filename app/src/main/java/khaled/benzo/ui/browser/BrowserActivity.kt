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
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import khaled.benzo.databinding.ActivityBrowserBinding
import khaled.benzo.ui.editor.EditorActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
        settings.allowFileAccess = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
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
