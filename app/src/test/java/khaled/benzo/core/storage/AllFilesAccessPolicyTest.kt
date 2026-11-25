package khaled.benzo.core.storage

import org.junit.Assert.*
import org.junit.Test

class AllFilesAccessPolicyTest {
    @Test fun notApplicableBeforeAndroid11() { assertFalse(AllFilesAccessPolicy.isApplicable(29)); assertTrue(AllFilesAccessPolicy.isApplicable(30)); assertTrue(AllFilesAccessPolicy.isApplicable(36)) }

    @Test fun offeredWhenApplicableNotGrantedNotDeclined() = assertTrue(AllFilesAccessPolicy.offerInFolderDialog(34, false, false))

    @Test fun notOfferedWhenGranted() = assertFalse(AllFilesAccessPolicy.offerInFolderDialog(34, true, false))

    @Test fun notOfferedWhenDeclinedBefore() = assertFalse(AllFilesAccessPolicy.offerInFolderDialog(34, false, true))

    @Test fun notOfferedBeforeAndroid11() = assertFalse(AllFilesAccessPolicy.offerInFolderDialog(28, false, false))
}
