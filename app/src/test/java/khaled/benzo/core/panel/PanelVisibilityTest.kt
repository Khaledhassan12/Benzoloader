package khaled.benzo.core.panel

import org.junit.Assert.*
import org.junit.Test

/** WRITTEN, NOT RUN. */
class PanelVisibilityTest {
    @Test fun toggleHidesThenShows() {
        val h = PanelVisibility.toggle(emptySet(), "qp_wifi_only")
        assertFalse(PanelVisibility.isShown(h, "qp_wifi_only"))
        assertTrue(PanelVisibility.isShown(PanelVisibility.toggle(h, "qp_wifi_only"), "qp_wifi_only"))
    }
    @Test fun cleanDropsUnknownKeys() = assertEquals(setOf("a"), PanelVisibility.clean(setOf("a", "zzz"), setOf("a", "b")))
}
