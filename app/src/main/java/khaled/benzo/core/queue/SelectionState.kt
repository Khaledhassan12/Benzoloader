package khaled.benzo.core.queue

/** Which rows are selected in the download list. Pure, so the rules are testable. Order of ids does not matter. */
data class SelectionState(val selected: Set<Long> = emptySet()) {
    val active: Boolean get() = selected.isNotEmpty()
    val count: Int get() = selected.size

    fun toggle(id: Long) = SelectionState(if (id in selected) selected - id else selected + id)
    fun selectAll(visible: Collection<Long>) = SelectionState(visible.toSet())
    fun invert(visible: Collection<Long>) = SelectionState(visible.filter { it !in selected }.toSet())
    /** Drops ids that are no longer in the list (deleted, or filtered out). Leaving no selection ends the mode. */
    fun prune(visible: Collection<Long>) = SelectionState(selected.intersect(visible.toSet()))
    fun clear() = SelectionState()
}
