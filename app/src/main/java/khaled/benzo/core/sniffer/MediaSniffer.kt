package khaled.benzo.core.sniffer

import android.webkit.JavascriptInterface
import android.webkit.WebView
import java.util.Locale

class MediaSniffer(
    private val onMediaFound: (DetectedMedia) -> Unit
) {

    private val mediaExtensions = setOf(
        "mp4", "mkv", "webm", "avi", "mov", "flv", "m3u8", "mpd", "mp3", "m4a", "aac", "ogg"
    )

    fun checkUrl(url: String, pageTitle: String?): Boolean {
        val clean = url.substringBefore('?').substringBefore('#').lowercase(Locale.ROOT)
        val ext = clean.substringAfterLast('.', "")
        if (ext in mediaExtensions) {
            val format = ext.uppercase(Locale.ROOT)
            val mimeType = when (ext) {
                "m3u8" -> "application/x-mpegURL"
                "mpd" -> "application/dash+xml"
                "mp4" -> "video/mp4"
                "webm" -> "video/webm"
                "mp3" -> "audio/mpeg"
                else -> "video/*"
            }
            onMediaFound(
                DetectedMedia(
                    url = url,
                    title = pageTitle ?: "Video_$format",
                    format = format,
                    mimeType = mimeType
                )
            )
            return true
        }
        return false
    }

    fun inspectUrl(url: String, headers: Map<String, String>? = null): Boolean {
        return checkUrl(url, null)
    }

    fun injectDetector(webView: WebView) {
        val js = """
            (function() {
                function inspect(elem) {
                    var src = elem.src || elem.currentSrc;
                    if (src && !src.startsWith('blob:') && !src.startsWith('data:')) {
                        window.BenzoSniffer.onDomMedia(src, document.title);
                    }
                }
                var tags = document.querySelectorAll('video, audio, source');
                for (var i = 0; i < tags.length; i++) {
                    inspect(tags[i]);
                }
                var obs = new MutationObserver(function(muts) {
                    muts.forEach(function(m) {
                        m.addedNodes.forEach(function(node) {
                            if (node.tagName === 'VIDEO' || node.tagName === 'AUDIO') inspect(node);
                        });
                    });
                });
                obs.observe(document.body, { childList: true, subtree: true });
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    @JavascriptInterface
    fun onDomMedia(src: String, title: String) {
        checkUrl(src, title)
    }
}
