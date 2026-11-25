package khaled.benzo.core.overlay

import khaled.benzo.core.download.SpeedTracker

/** What the Top window shows. Order and meaning follow the six statistics choices seen in the reference video. */
enum class TopStat { PERCENT, DOWNLOADED, SIZE, SPEED, ELAPSED, LEFT }

/** One running transfer, reduced to what the overlay needs. */
data class TopWindowSample(
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speedBytesPerSec: Long,
    val elapsedSeconds: Long
)

/** The Top window settings as stored. Colours are ARGB ints. Pixel values are raw pixels, as the reference rows show them. */
data class TopWindowConfig(
    val enabled: Boolean = false,
    val stats: Set<TopStat> = setOf(TopStat.PERCENT, TopStat.SPEED),
    val textColor: Int = 0xFFFFFFFF.toInt(),
    val backgroundColor: Int = 0xFF000000.toInt(),
    val barColor: Int = 0xFFE53935.toInt(),
    /** 10..100, how opaque the whole window is. */
    val opacityPercent: Int = 60,
    /** 0 = match the screen width. */
    val widthPx: Int = 0,
    val lineHeightPx: Int = 41,
    val textSizePx: Int = 14,
    val horizontal: Align = Align.START,
    val vertical: VAlign = VAlign.TOP,
    /** 0 = off. */
    val offsetXPx: Int = 0,
    val offsetYPx: Int = 0
) {
    enum class Align { START, CENTER, END }
    enum class VAlign { TOP, CENTER, BOTTOM }
}

object TopWindowSpec {
    /** The overlay only exists when it is switched on, permission is granted, and something is really transferring. */
    fun shouldShow(config: TopWindowConfig, canDrawOverlays: Boolean, runningCount: Int): Boolean =
        config.enabled && canDrawOverlays && runningCount > 0

    fun alphaFraction(opacityPercent: Int): Float = opacityPercent.coerceIn(10, 100) / 100f

    /** Progress 0..100 for the bar; unknown total shows an empty bar. */
    fun percent(sample: TopWindowSample): Int =
        if (sample.totalBytes <= 0L) 0 else ((sample.downloadedBytes * 100L) / sample.totalBytes).toInt().coerceIn(0, 100)

    /** Text line in a fixed order, items separated by a dot. Empty selection gives an empty string. */
    fun text(config: TopWindowConfig, s: TopWindowSample): String {
        val parts = ArrayList<String>()
        for (stat in TopStat.values()) {
            if (stat !in config.stats) continue
            parts += when (stat) {
                TopStat.PERCENT -> if (s.totalBytes > 0L) "%.1f%%".format(s.downloadedBytes * 100.0 / s.totalBytes) else "?"
                TopStat.DOWNLOADED -> SpeedTracker.formatSize(s.downloadedBytes)
                TopStat.SIZE -> if (s.totalBytes > 0L) SpeedTracker.formatSize(s.totalBytes) else "?"
                TopStat.SPEED -> SpeedTracker.formatSize(s.speedBytesPerSec) + "/s"
                TopStat.ELAPSED -> clock(s.elapsedSeconds)
                TopStat.LEFT -> if (s.totalBytes > 0L && s.speedBytesPerSec > 0L)
                    clock((s.totalBytes - s.downloadedBytes).coerceAtLeast(0L) / s.speedBytesPerSec) else "--:--"
            }
        }
        return parts.joinToString(" \u00B7 ")
    }

    fun clock(totalSeconds: Long): String {
        val t = totalSeconds.coerceAtLeast(0L)
        val h = t / 3600; val m = (t % 3600) / 60; val sec = t % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
    }
}
