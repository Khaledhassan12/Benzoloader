package khaled.benzo.core.notify

/** Buttons the ongoing download notification can carry, in display order. */
enum class NotifButton { PAUSE_ALL, EXIT }

/** What the lock screen may show of the ongoing notification. */
enum class LockScreenMode { FULL, HIDE_CONTENT }

/** Pure rules for the ongoing notification, driven by two user settings. Exit is always present. */
object NotificationPolicy {
    fun lockScreen(showOnLockScreen: Boolean): LockScreenMode =
        if (showOnLockScreen) LockScreenMode.FULL else LockScreenMode.HIDE_CONTENT

    fun buttons(pauseAllButton: Boolean): List<NotifButton> =
        if (pauseAllButton) listOf(NotifButton.PAUSE_ALL, NotifButton.EXIT) else listOf(NotifButton.EXIT)
}
