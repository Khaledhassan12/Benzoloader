package khaled.benzo.core.notify

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationPolicyTest {
    @Test fun lockScreenOffHidesContent() = assertEquals(LockScreenMode.HIDE_CONTENT, NotificationPolicy.lockScreen(false))
    @Test fun lockScreenOnShowsFull() = assertEquals(LockScreenMode.FULL, NotificationPolicy.lockScreen(true))
    @Test fun exitAlwaysThere() {
        assertEquals(listOf(NotifButton.EXIT), NotificationPolicy.buttons(false))
        assertEquals(listOf(NotifButton.STOP_ALL, NotifButton.EXIT), NotificationPolicy.buttons(true))
    }
}
