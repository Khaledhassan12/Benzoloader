package khaled.benzo.core.browser

/** Decides whether a WebView "page finished" event describes the page the single WebView shows now. */
object PageEventPolicy {
    /** True when the finished url is a real page and equals the url the WebView currently reports. */
    fun acceptFinish(finishedUrl: String?, currentUrl: String?): Boolean =
        finishedUrl != null && finishedUrl != "about:blank" && finishedUrl == currentUrl
}
