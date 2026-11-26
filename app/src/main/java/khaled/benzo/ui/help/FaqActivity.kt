package khaled.benzo.ui.help

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import khaled.benzo.R
import khaled.benzo.ui.common.CollapsingScreen

/** Offline FAQ page (HTML + CSS from assets), English or Arabic by the app language, coloured from the current theme. */
class FaqActivity : AppCompatActivity() {
    private fun color(attr: Int, fallback: Int): Int {
        val tv = TypedValue()
        return if (theme.resolveAttribute(attr, tv, true) && tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT) tv.data else fallback
    }

    private fun css(c: Int) = String.format("#%06X", c and 0xFFFFFF)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val web = WebView(this)
        // Offline page: no network, no scripts, no file access.
        web.settings.javaScriptEnabled = false
        web.settings.blockNetworkLoads = true
        web.settings.allowFileAccess = false
        web.settings.allowContentAccess = false
        val bg = color(android.R.attr.colorBackground, 0xFF121212.toInt())
        val on = color(com.google.android.material.R.attr.colorOnSurface, 0xFFFFFFFF.toInt())
        val primary = color(androidx.appcompat.R.attr.colorPrimary, 0xFF80CBC4.toInt())
        val card = color(com.google.android.material.R.attr.colorSurfaceContainer, ColorUtils.blendARGB(bg, on, 0.08f))
        val chip = color(com.google.android.material.R.attr.colorPrimaryContainer, ColorUtils.blendARGB(bg, primary, 0.25f))
        val onChip = color(com.google.android.material.R.attr.colorOnPrimaryContainer, on)
        val muted = color(com.google.android.material.R.attr.colorOnSurfaceVariant, ColorUtils.blendARGB(bg, on, 0.7f))
        val line = ColorUtils.blendARGB(bg, on, 0.14f)
        web.setBackgroundColor(bg)
        val lang = resources.configuration.locales[0].language
        val file = if (lang == "ar") "faq_ar.html" else "faq_en.html"
        val html = assets.open(file).bufferedReader().use { it.readText() }
            .replace("{{bg}}", css(bg)).replace("{{card}}", css(card)).replace("{{primary}}", css(primary))
            .replace("{{on}}", css(on)).replace("{{muted}}", css(muted)).replace("{{chip}}", css(chip))
            .replace("{{onchip}}", css(onChip)).replace("{{line}}", css(line))
        web.webViewClient = object : WebViewClient() {
            // Links open in the browser app, never inside this page.
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(request.url.toString()))) } catch (_: Exception) {}
                return true
            }
        }
        web.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
        setContentView(CollapsingScreen.wrap(this, getString(R.string.help_faq), web))
    }
}
