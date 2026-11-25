package khaled.benzo.core.data

/**
 * Pure logic for the "what to export" checkbox dialog (reference: six entries, OK dimmed with
 * nothing ticked, OK carries a count when the finished entry is ticked). Written, not built.
 */
enum class ExportItem { QUEUE, FINISHED, BOOKMARKS, HISTORY, SITE_MANAGER, AUTHORIZATION }

data class ExportSelection(val ticked: Set<ExportItem> = emptySet()) {
    fun toggle(item: ExportItem): ExportSelection =
        copy(ticked = if (item in ticked) ticked - item else ticked + item)

    /** OK is enabled only when at least one entry is ticked. */
    val okEnabled: Boolean get() = ticked.isNotEmpty()

    /** Number shown beside OK: sum of counts of ticked entries that have a count. */
    fun okCount(counts: Map<ExportItem, Int>): Int = ticked.sumOf { counts[it] ?: 0 }

    /**
     * Credentials must never be written in clear. Until a safe design is approved the
     * AUTHORIZATION entry is excluded from what is actually written.
     */
    fun writable(): Set<ExportItem> = ticked - ExportItem.AUTHORIZATION
}
