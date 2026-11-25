package khaled.benzo.core.torrent

import org.junit.Assert.assertEquals
import org.junit.Test

class TorrentNetworkPolicyTest {
    @Test fun zeroKeepsLibraryDefault() = assertEquals(200, TorrentNetworkPolicy.connectionsToApply(0, 200))
    @Test fun valueIsUsedWhenInRange() = assertEquals(300, TorrentNetworkPolicy.connectionsToApply(300, 200))
    @Test fun portIsUsedOnlyWhenFixedAndInRange() {
        assertEquals(0, TorrentNetworkPolicy.portToApply(false, 40000))
        assertEquals(40000, TorrentNetworkPolicy.portToApply(true, 40000))
        assertEquals(0, TorrentNetworkPolicy.portToApply(true, 80))
        assertEquals(0, TorrentNetworkPolicy.portToApply(true, 70000))
    }
    @Test fun listenInterfacesCoverBothFamilies() = assertEquals("0.0.0.0:40000,[::]:40000", TorrentNetworkPolicy.listenInterfaces(40000))
    @Test fun valueIsBounded() {
        assertEquals(1000, TorrentNetworkPolicy.connectionsToApply(5000, 200))
        assertEquals(10, TorrentNetworkPolicy.connectionsToApply(3, 200))
    }
}
