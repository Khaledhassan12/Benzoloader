package khaled.benzo.core.queue

import khaled.benzo.data.model.FileCategory

/** Everything the main list needs to decide what to show, with the transitions the screen can make. */
data class ListState(
    val status: StatusFilter = StatusFilter.ALL,
    val type: FileCategory? = null,
    val query: String = "",
    val sort: SortKey = SortKey.TIME,
    val ascending: Boolean = false,
    /** Display order of new downloads: false = newest on top, true = newest at the bottom. Not the queue start order. */
    val addToBottom: Boolean = false,
    val time: TimeFilter = TimeFilter.ALL
) {
    /** Panel versions: change one dimension and keep the others, unlike the drawer entries above which replace the status or type. */
    fun withStatusOnly(filter: StatusFilter): ListState = copy(status = filter)
    fun withTypeOnly(category: FileCategory?): ListState = copy(type = category?.takeIf { it != FileCategory.ALL })
    fun withSortDirection(key: SortKey, ascending: Boolean): ListState = copy(sort = key, ascending = ascending)
    fun cleared(): ListState = copy(status = StatusFilter.ALL, type = null, time = TimeFilter.ALL)

    fun withTime(filter: TimeFilter): ListState = copy(time = filter)

    /** Applies the stored display-order choice. Only the time sort follows it, and only while the user has not flipped it by hand. */
    fun withAddToBottom(bottom: Boolean): ListState =
        if (bottom == addToBottom) this
        else copy(addToBottom = bottom, ascending = if (sort == SortKey.TIME) bottom else ascending)

    /** Drawer entries: ALL and the status entries set a status filter, the others filter by file type. */
    fun withCategory(category: FileCategory?): ListState = when (category) {
        null, FileCategory.ALL -> copy(status = StatusFilter.ALL, type = null)
        FileCategory.DOWNLOADING -> copy(status = StatusFilter.ACTIVE, type = null)
        FileCategory.COMPLETED -> copy(status = StatusFilter.COMPLETED, type = null)
        FileCategory.QUEUED -> copy(status = StatusFilter.QUEUED, type = null)
        else -> copy(status = StatusFilter.ALL, type = category)
    }

    fun withStatus(filter: StatusFilter): ListState = copy(status = filter, type = null)

    fun withQuery(query: String): ListState = copy(query = query)

    /** Picking the active sort key again flips the direction; a new key starts in its natural direction. */
    fun withSort(key: SortKey): ListState =
        if (sort == key) copy(ascending = !ascending)
        else copy(sort = key, ascending = key == SortKey.NAME || key == SortKey.CATEGORY || (key == SortKey.TIME && addToBottom))

    /** Applies filters, search, sort and the file-type filter to the full list. */
    fun apply(all: List<khaled.benzo.data.model.DownloadItem>, nowMillis: Long = System.currentTimeMillis()): List<khaled.benzo.data.model.DownloadItem> =
        ListLogic.apply(all, status, query, sort, ascending, time, nowMillis).let { list -> if (type == null) list else list.filter { it.category == type } }
}
