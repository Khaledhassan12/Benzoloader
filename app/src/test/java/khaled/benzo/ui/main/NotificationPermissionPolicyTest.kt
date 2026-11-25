package khaled.benzo.ui.main

import org.junit.Assert.*
import org.junit.Test

class NotificationPermissionPolicyTest {
    private fun n(sdk: Int, g: Boolean, r: Boolean) = NotificationPermissionPolicy.next(sdk, g, r)

    @Test fun beforeAndroid13NothingIsAsked() = assertEquals(NotificationStep.NOTHING, n(32, false, false))
    @Test fun alreadyGrantedAsksNothing() = assertEquals(NotificationStep.NOTHING, n(34, true, false))
    @Test fun firstTimeAsksDirectly() = assertEquals(NotificationStep.ASK, n(33, false, false))
    @Test fun afterOneDenialExplainsFirst() = assertEquals(NotificationStep.EXPLAIN_THEN_ASK, n(34, false, true))
    @Test fun settingsOnlyWhenDeniedAndSystemWillNotAskAgain() {
        assertTrue(NotificationPermissionPolicy.offerSettings(false, false))
        assertFalse(NotificationPermissionPolicy.offerSettings(false, true))
        assertFalse(NotificationPermissionPolicy.offerSettings(true, false))
    }
}
