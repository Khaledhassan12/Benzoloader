package khaled.benzo.core.queue

/**
 * Decides whether the Editor may send its small size/name request before Start, and keeps results tied to the current link.
 * The request follows the same network rules as the download: no network, or a Wi-Fi-only rule (this download's box or the
 * global one) while not on Wi-Fi, means no request. There is no app proxy yet; when one exists, the probe must use the same
 * client so it can never go around it (tracked in the log).
 */
object ProbePolicy {
    enum class Decision { ALLOW, NO_NETWORK, WAITING_FOR_WIFI }

    fun decide(networkAvailable: Boolean, onWifi: Boolean, thisWifiOnly: Boolean, globalWifiOnly: Boolean): Decision = when {
        !networkAvailable -> Decision.NO_NETWORK
        (thisWifiOnly || globalWifiOnly) && !onWifi -> Decision.WAITING_FOR_WIFI
        else -> Decision.ALLOW
    }
}

/** One probe per distinct link; a result is applied only if its link is still the current one. */
class ProbeGate {
    private var lastStarted: String? = null

    /** False when this exact link was already probed (a programmatic re-set of the same text). [force] re-probes after a rule change. */
    fun begin(url: String, force: Boolean = false): Boolean {
        if (!force && url == lastStarted) return false
        lastStarted = url
        return true
    }

    fun isCurrent(requested: String, currentText: String): Boolean = requested == currentText.trim()
}
