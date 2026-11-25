package khaled.benzo.core.storage

import khaled.benzo.core.storage.LegacyStoragePolicy.Step
import org.junit.Assert.*
import org.junit.Test

class LegacyStoragePolicyTest {
    @Test fun nothingOnAndroid11AndNewer() = assertEquals(Step.NOTHING, LegacyStoragePolicy.next(30, false, false))

    @Test fun nothingWhenGranted() = assertEquals(Step.NOTHING, LegacyStoragePolicy.next(28, true, false))

    @Test fun asksOnAndroid10AndOlder() { assertEquals(Step.ASK, LegacyStoragePolicy.next(29, false, false)); assertEquals(Step.ASK, LegacyStoragePolicy.next(26, false, false)) }

    @Test fun explainsWhenRationaleIsNeeded() = assertEquals(Step.EXPLAIN_THEN_ASK, LegacyStoragePolicy.next(27, false, true))

    @Test fun settingsOfferedAfterPermanentDenial() { assertTrue(LegacyStoragePolicy.offerSettings(false, false)); assertFalse(LegacyStoragePolicy.offerSettings(true, false)); assertFalse(LegacyStoragePolicy.offerSettings(false, true)) }

    @Test fun dialogOfferOnlyWhenApplicableAndMissing() {
        assertTrue(LegacyStoragePolicy.offerInFolderDialog(29, false)); assertFalse(LegacyStoragePolicy.offerInFolderDialog(29, true)); assertFalse(LegacyStoragePolicy.offerInFolderDialog(30, false))
    }
}
