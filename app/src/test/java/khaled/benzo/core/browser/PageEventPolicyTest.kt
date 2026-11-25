package khaled.benzo.core.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PageEventPolicyTest {
    @Test fun acceptsCurrentPage() = assertTrue(PageEventPolicy.acceptFinish("https://a.test/", "https://a.test/"))
    @Test fun rejectsStalePage() = assertFalse(PageEventPolicy.acceptFinish("https://old.test/", "https://new.test/"))
    @Test fun rejectsBlankAndNull() {
        assertFalse(PageEventPolicy.acceptFinish("about:blank", "about:blank"))
        assertFalse(PageEventPolicy.acceptFinish(null, "https://a.test/"))
    }
}
