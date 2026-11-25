package khaled.benzo.core.torrent

/**
 * When a finished torrent should stop uploading. Pure logic.
 * [ratioTenths]: stop when uploaded / size reaches this ratio times ten (15 = 1.5). 0 = no ratio limit.
 * [minutes]: stop after seeding this long. 0 = no time limit. When both are set, the first one reached stops it.
 */
object SeedingPolicy {
    enum class Reason { RATIO, TIME }

    fun stopReason(ratioTenths: Int, minutes: Int, uploadedBytes: Long, sizeBytes: Long, seedingSeconds: Long): Reason? {
        if (ratioTenths > 0 && sizeBytes > 0 && uploadedBytes.coerceAtLeast(0L) * 10L >= ratioTenths.toLong() * sizeBytes) return Reason.RATIO
        if (minutes > 0 && seedingSeconds >= minutes.toLong() * 60L) return Reason.TIME
        return null
    }
}
