package khaled.benzo.core.queue

import khaled.benzo.data.model.FileCategory

/** Everything the main list needs to decide what to show, with the transitions the screen can make. */
data class ListState(
    val status: StatusFilter = StatusFilter.ALL,
    val type: FileCategory? = null,
    val query: String = "",
    val sort: SortKey = SortKey.TIME,
    val ascending: Boolean = false
) {
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
        else copy(sort = key, ascending = key == SortKey.NAME || key == SortKey.CATEGORY)

    /** Applies filters, search, sort and the file-type filter to the full list. */
    fun apply(all: List<khaled.benzo.data.model.DownloadItem>): List<khaled.benzo.data.model.DownloadItem> =
        ListLogic.apply(all, status, query, sort, ascending).let { list -> if (type == null) list else list.filter { it.category == type } }
}
