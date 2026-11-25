package khaled.benzo.core.torrent

/**
 * Encryption mode choices seen in the reference (radio: prefer, require, disable). Pure mapping, no native classes.
 * The stored setting is the index (0 prefer, 1 require, 2 disable). Prefer is also the library's own default.
 */
enum class TorrentEncryption(val index: Int) {
    PREFER(0), REQUIRE(1), DISABLE(2);

    /** The reference dims the separate incoming/outgoing connection rows when encryption is disabled (OBSERVED, video 5). */
    val dimsConnectionRows: Boolean get() = this == DISABLE

    companion object {
        fun fromIndex(i: Int): TorrentEncryption = values().firstOrNull { it.index == i } ?: PREFER
    }
}
