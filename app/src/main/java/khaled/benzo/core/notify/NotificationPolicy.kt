package khaled.benzo.core.notify

/** Buttons the ongoing download notification can carry, in display order. */
enum class NotifButton(val key: String) {
    // PAUSE_ALL and STOP_ALL are the same action; PAUSE_ALL is the older name kept so older code and tests still compile.
    START_ALL("start_all"), PAUSE_ALL("stop_all"), ADD("add"), SCHEDULER("scheduler"), SETTINGS("settings"), BROWSER("browser"), EXIT("exit");
    companion object { fun fromKey(k: String) = values().firstOrNull { it.key == k } }
}

/** What the lock screen may show of the ongoing notification. */
enum class LockScreenMode { FULL, HIDE_CONTENT }

/** Pure rules for the ongoing notification, driven by two user settings. Exit is always present. */
object NotificationPolicy {
    fun lockScreen(showOnLockScreen: Boolean): LockScreenMode =
        if (showOnLockScreen) LockScreenMode.FULL else LockScreenMode.HIDE_CONTENT

    fun buttons(pauseAllButton: Boolean): List<NotifButton> =
        if (pauseAllButton) listOf(NotifButton.PAUSE_ALL, NotifButton.EXIT) else listOf(NotifButton.EXIT)

    /**
     * Android shows at most this many action buttons on a notification, so the picker cannot offer more. The reference
     * dialog lists seven entries; whether it can show more than three at once was not seen (UNCLEAR).
     */
    const val MAX_BUTTONS = 3

    /** Buttons to show. [stored] is the saved comma list (null = never chosen, so the older Pause all switch and Exit apply). */
    fun commandButtons(stored: String?, legacyPauseAll: Boolean): List<NotifButton> {
        if (stored == null) return buttons(legacyPauseAll)
        return stored.split(',').mapNotNull { NotifButton.fromKey(it.trim()) }.distinct().take(MAX_BUTTONS)
    }

    /** Result of ticking or unticking [b] in the picker: the new list, or null when ticking would pass [MAX_BUTTONS]. */
    fun toggled(current: List<NotifButton>, b: NotifButton): List<NotifButton>? =
        if (b in current) current - b else if (current.size >= MAX_BUTTONS) null else current + b

    fun format(list: List<NotifButton>): String = list.joinToString(",") { it.key }
}
