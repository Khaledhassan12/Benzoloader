package khaled.benzo.core.settings

import khaled.benzo.core.settings.SettingsBackup as B
import org.junit.Assert.*
import org.junit.Test

class SettingsBackupTest {
    private val sample: Map<String, Any?> = mapOf(
        "max_concurrent" to 5, "battery_threshold" to 15, "wifi_only" to true, "speed_limit" to 131072L,
        "catalog_folder_names" to "video=Films\nmusic=Songs", "download_path" to "/storage/emulated/0/Download/ADM"
    )

    @Test fun roundTrip() {
        val r = B.parse(B.format(sample))
        assertTrue(r.isSettingsFile)
        assertEquals(sample.mapValues { it.value!! }, r.accepted)
        assertEquals(0, r.ignored); assertEquals(0, r.invalid)
    }

    @Test fun formatSkipsUnknownKeysAndWrongTypes() {
        val t = B.format(mapOf("secret_token" to "x", "max_concurrent" to "5", "wifi_only" to true))
        assertFalse(t.contains("secret_token")); assertFalse(t.contains("max_concurrent")); assertTrue(t.contains("wifi_only=true"))
    }

    @Test fun notASettingsFileIsRejectedWhole() {
        val r = B.parse("wifi_only=true\n")
        assertFalse(r.isSettingsFile); assertTrue(r.accepted.isEmpty())
        assertFalse(B.parse("").isSettingsFile)
    }

    @Test fun unknownKeysAreCountedAndNotAccepted() {
        val r = B.parse("${B.MAGIC}\nunknown=1\nwifi_only=true\n")
        assertEquals(mapOf<String, Any>("wifi_only" to true), r.accepted); assertEquals(1, r.ignored)
    }

    @Test fun outOfRangeAndMalformedValuesAreInvalid() {
        val r = B.parse("${B.MAGIC}\nmax_concurrent=999\nbattery_threshold=7\nwifi_only=yes\nerror_retries=abc\nno equals sign\nschedule_start_minute=1440\n")
        assertTrue(r.accepted.isEmpty()); assertEquals(6, r.invalid)
    }

    @Test fun boundariesAreAccepted() {
        val r = B.parse("${B.MAGIC}\nmax_concurrent=1\nmax_concurrent=5\nschedule_stop_minute=1439\nbattery_threshold=0\n")
        assertEquals(5, r.accepted["max_concurrent"]); assertEquals(1439, r.accepted["schedule_stop_minute"]); assertEquals(0, r.accepted["battery_threshold"])
    }

    @Test fun crlfBomCommentsAndBlankLines() {
        val r = B.parse("\uFEFF${B.MAGIC}\r\n\r\n# note\r\nwifi_only=false\r\n")
        assertEquals(mapOf<String, Any>("wifi_only" to false), r.accepted); assertEquals(0, r.invalid)
    }

    @Test fun textEscapesNewlinesAndBackslashes() {
        val v = "a\\b\nc"
        assertEquals(v, B.parse(B.format(mapOf("catalog_folder_names" to v))).accepted["catalog_folder_names"])
    }

    @Test fun sliderStepsAreEnforced() {
        val r = B.parse("${B.MAGIC}\nbattery_threshold=4\nbattery_threshold=55\nspeed_limit=1000\nspeed_limit=65536\n")
        assertEquals(3, r.invalid); assertEquals(65536L, r.accepted["speed_limit"]); assertNull(r.accepted["battery_threshold"])
        assertEquals(50, B.parse("${B.MAGIC}\nbattery_threshold=50\n").accepted["battery_threshold"])
    }

    @Test fun badEscapeIsInvalid() = assertEquals(1, B.parse("${B.MAGIC}\ncatalog_folder_names=bad\\x\n").invalid)

    @Test fun pathRules() {
        assertTrue(B.isPlausiblePath("")); assertTrue(B.isPlausiblePath("/storage/emulated/0/Download"))
        assertFalse(B.isPlausiblePath("relative/x")); assertFalse(B.isPlausiblePath("/a/../b")); assertFalse(B.isPlausiblePath("/a\nb"))
        assertTrue(B.isPlausiblePath("/a/..b/c"))
        assertEquals(1, B.parse("${B.MAGIC}\ndownload_path=/x/../y\n").invalid)
    }

    @Test fun everySpecIsExportable() {
        // a value of the right type for each spec formats and parses back unchanged
        val values = B.specs.mapValues { (_, s) -> when (s) {
            is B.Spec.Bool -> true; is B.Spec.IntRange -> s.min; is B.Spec.LongRange -> s.min; is B.Spec.Text -> ""
        } }
        assertEquals(values, B.parse(B.format(values)).accepted)
    }

    /** WRITTEN, NOT RUN: the newer persisted switches are covered by the backup, and a non-boolean value is rejected. */
    @Test fun emptyFileAndGraphSwitchesRoundTrip() {
        val text = B.MAGIC + "\ncreate_empty_file=false\nquick_graph_visible=true\n"
        val r = B.parse(text)
        assertEquals(false, r.accepted["create_empty_file"])
        assertEquals(true, r.accepted["quick_graph_visible"])
        assertEquals(1, B.parse(B.MAGIC + "\ncreate_empty_file=maybe\n").invalid)
    }

    /** WRITTEN, NOT RUN. */
    @Test fun hiddenPanelRowsAreTextWithSafeCharactersOnly() {
        assertEquals("qp_wifi_only,qp_theme", B.parse(B.MAGIC + "\nquick_panel_hidden=qp_wifi_only,qp_theme\n").accepted["quick_panel_hidden"])
        assertEquals(1, B.parse(B.MAGIC + "\nquick_panel_hidden=bad value!\n").invalid)
    }
}
