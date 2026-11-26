package khaled.benzo.core.panel

/**
 * Which quick-panel rows the user hid with the eye editor. Pure and testable.
 * OBSERVED (video 3 part 1): the eye in a section header shows a checkbox beside every row, and unticking rows changes which
 * rows the panel shows. NOT SEEN: whether the choice survives leaving edit mode. I keep it saved, which is my choice.
 */
object PanelVisibility {
    fun isShown(hidden: Set<String>, key: String) = key !in hidden
    fun toggle(hidden: Set<String>, key: String): Set<String> = if (key in hidden) hidden - key else hidden + key
    /** Keep only keys the panel really has, so a stale or hand-edited value cannot hide a row nobody can bring back. */
    fun clean(stored: Set<String>, known: Set<String>): Set<String> = stored.filter { it in known }.toSet()
}
