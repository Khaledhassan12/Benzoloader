package khaled.benzo.core.notify

/** How long to vibrate for an event. 0 means do not vibrate. Durations are clamped so a bad stored value cannot buzz for long. */
object VibrationPolicy {
    const val MIN_MS = 50
    const val MAX_MS = 1000

    fun durationMs(masterEnabled: Boolean, configuredMs: Int): Long =
        if (!masterEnabled) 0L else configuredMs.coerceIn(MIN_MS, MAX_MS).toLong()
}
