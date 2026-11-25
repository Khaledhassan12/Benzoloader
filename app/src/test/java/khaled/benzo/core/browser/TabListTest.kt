package khaled.benzo.core.browser

import org.junit.Assert.*
import org.junit.Test

class TabListTest {
    @Test fun startsWithOneSelectedTab() {
        val t = TabList.single("https://a.com")
        assertEquals(1, t.tabs.size); assertEquals("https://a.com", t.selected.url)
    }

    @Test fun openSelectsTheNewTab() {
        val t = TabList.single().open("https://b.com")
        assertEquals(2, t.tabs.size); assertEquals("https://b.com", t.selected.url)
    }

    @Test fun maxTabsIsEnforced() {
        var t = TabList.single()
        repeat(40) { t = t.open("https://x/$it") }
        assertEquals(TabList.MAX_TABS, t.tabs.size)
    }

    @Test fun closingTheSelectedMiddleTabSelectsTheNextOne() {
        val t = TabList.single("a").open("b").open("c").select(2).close(2)
        assertEquals(listOf("a", "c"), t.tabs.map { it.url })
        assertEquals("c", t.selected.url)
    }

    @Test fun closingTheSelectedLastTabSelectsThePreviousOne() {
        val t = TabList.single("a").open("b").close(2)
        assertEquals("a", t.selected.url)
    }

    @Test fun closingAnotherTabKeepsTheSelection() {
        val t = TabList.single("a").open("b").open("c").close(1)
        assertEquals("c", t.selected.url); assertEquals(2, t.tabs.size)
    }

    @Test fun closingTheOnlyTabLeavesABlankOne() {
        val t = TabList.single("a").close(1)
        assertEquals(1, t.tabs.size); assertEquals("", t.selected.url)
        assertNotEquals(1, t.selected.id)
    }

    @Test fun ids_areNotReusedAfterClose() {
        val t = TabList.single("a").open("b").close(2).open("c")
        assertEquals(listOf(1, 3), t.tabs.map { it.id })
    }

    @Test fun updateChangesOnlyThatTab() {
        val t = TabList.single("a").open("b").update(1, "a2", "A")
        assertEquals("a2", t.tabs[0].url); assertEquals("A", t.tabs[0].title); assertEquals("b", t.tabs[1].url)
    }

    @Test fun unknownIdChangesNothing() {
        val t = TabList.single("a")
        assertEquals(t, t.select(99)); assertEquals(t, t.close(99))
    }

    @Test fun serializeRoundTripKeepsWebTabsAndSelection() {
        val t = TabList.single("https://a.com").open("").open("https://b.com\tx").update(3, null, "T\nx").select(1)
        val back = TabList.parse(t.serialize())
        assertEquals(listOf("https://a.com", "https://b.com x"), back.tabs.map { it.url })
        assertEquals("https://a.com", back.selected.url)
        assertEquals("T x", back.tabs[1].title)
    }

    @Test fun badTextGivesASingleTab() {
        assertEquals(1, TabList.parse("junk").tabs.size)
        assertEquals("https://f.com", TabList.parse("", "https://f.com").selected.url)
        assertEquals(1, TabList.parse("0\nabout:blank\t").tabs.size)
    }
}
