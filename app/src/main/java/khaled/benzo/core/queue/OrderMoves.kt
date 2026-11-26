package khaled.benzo.core.queue

/** Hand moves in the Order sort: To top, To bottom, Up, Down. All in screen terms (top of the screen is "top"). Pure. */
object OrderMoves {
    enum class Move { TOP, BOTTOM, UP, DOWN }

    /** [screen] is the displayed order top to bottom. Selected items keep their relative order. Returns the new screen order. */
    fun move(screen: List<Long>, selected: Set<Long>, m: Move): List<Long> {
        val sel = screen.filter { it in selected }
        if (sel.isEmpty()) return screen
        return when (m) {
            Move.TOP -> sel + screen.filter { it !in selected }
            Move.BOTTOM -> screen.filter { it !in selected } + sel
            Move.UP -> { val l = screen.toMutableList(); for (i in 1 until l.size) if (l[i] in selected && l[i - 1] !in selected) { val t = l[i]; l[i] = l[i - 1]; l[i - 1] = t }; l }
            Move.DOWN -> { val l = screen.toMutableList(); for (i in l.size - 2 downTo 0) if (l[i] in selected && l[i + 1] !in selected) { val t = l[i]; l[i] = l[i + 1]; l[i + 1] = t }; l }
        }
    }

    /**
     * Puts a moved subset back into the full list. Hidden items keep their exact slots; the subset's items fill the slots the
     * subset occupied, in their new order. This is how a move under a filter leaves everything outside the filter alone.
     */
    fun splice(fullBefore: List<Long>, subsetBefore: List<Long>, subsetAfter: List<Long>): List<Long> {
        require(subsetBefore.toSet() == subsetAfter.toSet() && subsetBefore.size == subsetAfter.size) { "subset changed" }
        val inSubset = subsetBefore.toHashSet(); val it = subsetAfter.iterator()
        return fullBefore.map { id -> if (id in inSubset) it.next() else id }
    }

    /**
     * New order keys for the displayed items. The same set of keys is handed out again in the new order, so keys stay in
     * their old range (a new download still lands where it did). Keys are made strictly increasing. Returns only the changed ones.
     * [ascending] is how the screen is sorted: true = smaller key higher on the screen.
     * Pass the FULL list (every item), never a filtered one, so no hidden item can end up with the same key as a shown one.
     */
    fun newKeys(screenBefore: List<Long>, screenAfter: List<Long>, keyOf: Map<Long, Long>, ascending: Boolean): Map<Long, Long> {
        val sorted = screenBefore.map { keyOf.getValue(it) }.sorted()
        val strict = LongArray(sorted.size)
        for (i in sorted.indices) strict[i] = if (i == 0) sorted[0] else maxOf(sorted[i], strict[i - 1] + 1)
        val byRank = if (ascending) screenAfter else screenAfter.reversed()
        val out = LinkedHashMap<Long, Long>()
        byRank.forEachIndexed { i, id -> if (keyOf.getValue(id) != strict[i]) out[id] = strict[i] }
        return out
    }

    /**
     * "Apply sorting": store the order the current sort shows as the list order. INFERRED from the reference (video 2 dense
     * frames: after OK the Order (down) row is selected and the list keeps the order the sort produced); not seen to be stored.
     * [orderScreen] is the full list in the Order sort, descending (the Order (down) screen), [sortedScreen] the full list as
     * the chosen sort shows it. Returns only the changed keys.
     */
    fun applySorted(orderScreen: List<Long>, sortedScreen: List<Long>, keyOf: Map<Long, Long>): Map<Long, Long> =
        newKeys(orderScreen, sortedScreen, keyOf, ascending = false)
}
