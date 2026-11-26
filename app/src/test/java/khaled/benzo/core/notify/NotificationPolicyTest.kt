package khaled.benzo.core.notify

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationPolicyTest {
    @Test fun lockScreenOffHidesContent() = assertEquals(LockScreenMode.HIDE_CONTENT, NotificationPolicy.lockScreen(false))
    @Test fun lockScreenOnShowsFull() = assertEquals(LockScreenMode.FULL, NotificationPolicy.lockScreen(true))
    @Test fun exitAlwaysThere() {
        assertEquals(listOf(NotifButton.EXIT), NotificationPolicy.buttons(false))
        assertEquals(listOf(NotifButton.PAUSE_ALL, NotifButton.EXIT), NotificationPolicy.buttons(true))
    }

    /** WRITTEN, NOT RUN. */
    @Test fun commandButtonsFallBackToTheOlderSwitch() {
        assertEquals(listOf(NotifButton.EXIT), NotificationPolicy.commandButtons(null, false))
        assertEquals(listOf(NotifButton.PAUSE_ALL, NotifButton.EXIT), NotificationPolicy.commandButtons(null, true))
    }
    @Test fun storedListWinsAndIsCapped() {
        assertEquals(listOf(NotifButton.SETTINGS), NotificationPolicy.commandButtons("settings", true))
        assertEquals(emptyList<NotifButton>(), NotificationPolicy.commandButtons("", true))
        assertEquals(3, NotificationPolicy.commandButtons("add,settings,browser,exit", false).size)
        assertEquals(listOf(NotifButton.ADD), NotificationPolicy.commandButtons("add,bogus,add", false))
    }
    @Test fun toggleRespectsTheCap() {
        val three = listOf(NotifButton.ADD, NotifButton.SETTINGS, NotifButton.BROWSER)
        assertNull(NotificationPolicy.toggled(three, NotifButton.EXIT))
        assertEquals(listOf(NotifButton.ADD, NotifButton.BROWSER), NotificationPolicy.toggled(three, NotifButton.SETTINGS))
        assertEquals(listOf(NotifButton.EXIT), NotificationPolicy.toggled(emptyList(), NotifButton.EXIT))
    }
    @Test fun formatRoundTrips() = assertEquals("add,exit", NotificationPolicy.format(listOf(NotifButton.ADD, NotifButton.EXIT)))
}
