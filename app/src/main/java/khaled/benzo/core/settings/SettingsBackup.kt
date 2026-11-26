package khaled.benzo.core.settings

/**
 * Settings export and import (settings only: no downloads, cookies, headers, site passwords, history or bookmarks).
 * The file is plain text, one `key=value` per line after a version line. Only the settings listed in [specs] are read or
 * written, each with its own type and range, so a damaged or hand-edited file cannot set anything else or an absurd value.
 * Pure logic with no Android classes.
 */
object SettingsBackup {
    const val MAGIC = "# Benzoloader settings v1"

    sealed class Spec {
        object Bool : Spec()
        /** [step] > 1: the value must be min + k*step (the settings sliders refuse other values). [zeroOk]: 0 is also allowed (means off). */
        data class IntRange(val min: Int, val max: Int, val step: Int = 1, val zeroOk: Boolean = false) : Spec()
        data class LongRange(val min: Long, val max: Long, val step: Long = 1L) : Spec()
        data class Text(val maxLength: Int, val check: (String) -> Boolean = { true }) : Spec()
    }

    val specs: Map<String, Spec> = linkedMapOf(
        "max_concurrent" to Spec.IntRange(1, 5),
        "default_threads" to Spec.IntRange(1, 16),
        "wifi_only" to Spec.Bool,
        "speed_limit" to Spec.LongRange(0L, 10240L * 1024L, 64L * 1024L),
        "battery_threshold" to Spec.IntRange(5, 50, 5, zeroOk = true),
        "stop_on_unplug" to Spec.Bool,
        "sound_complete" to Spec.Bool,
        "vibrate_complete" to Spec.Bool,
        "schedule_enabled" to Spec.Bool,
        "schedule_start_minute" to Spec.IntRange(0, 1439),
        "schedule_stop_minute" to Spec.IntRange(0, 1439),
        "catalog_enabled" to Spec.Bool,
        "catalog_folder_names" to Spec.Text(2000),
        "error_retries" to Spec.IntRange(0, 128),
        "pause_after_error" to Spec.Bool,
        "auto_resume" to Spec.Bool,
        "notif_lock_screen" to Spec.Bool,
        "notif_pause_all_button" to Spec.Bool,
        "keep_screen_on" to Spec.Bool,
        "clear_finished_on_start" to Spec.Bool,
        "torrent_sequential" to Spec.Bool,
        "torrent_seed_after_finish" to Spec.Bool,
        "torrent_seed_ratio_tenths" to Spec.IntRange(0, 100, 5),
        "torrent_seed_minutes" to Spec.IntRange(0, 4320, 30),
        "torrent_dht" to Spec.Bool,
        "newest_first" to Spec.Bool,
        "after_finish" to Spec.IntRange(0, 1, 1),
        "quick_add_links" to Spec.Bool,
        "quick_add_start" to Spec.Bool,
        "list_add_to_bottom" to Spec.Bool,
        "swap_side_menus" to Spec.Bool,
        "torrent_fixed_port" to Spec.Bool,
        "torrent_port" to Spec.IntRange(1024, 65535, 1),
        "torrent_anonymous" to Spec.Bool,
        "vibrate_success_ms" to Spec.IntRange(50, 1000, 50),
        "vibrate_on_error" to Spec.Bool,
        "clear_notifs_on_exit" to Spec.Bool,
        "vibrate_error_ms" to Spec.IntRange(50, 1000, 50),
        "min_thread_kb" to Spec.IntRange(128, 4096, 128),
        "default_user_agent" to Spec.Text(300) { s -> s.none { it.code < 32 || it.code > 126 } },
        "torrent_connections" to Spec.IntRange(50, 1000, 50, zeroOk = true),
        "browser_history" to Spec.Bool,
        "browser_save_tabs" to Spec.Bool,
        "dark_mode" to Spec.Bool,
        "bottom_bar" to Spec.Bool,
        "create_empty_file" to Spec.Bool,
        "notif_buttons" to Spec.Text(100) { s -> s.all { it in 'a'..'z' || it == '_' || it == ',' } },
        "vib_total_ms" to Spec.IntRange(50, 1000, 50),
        "quick_graph_visible" to Spec.Bool,
        "stat_added_time" to Spec.Bool,
        "stat_finish_time" to Spec.Bool,
        "ui_col_filename" to Spec.IntRange(Int.MIN_VALUE, Int.MAX_VALUE),
        "ui_col_info" to Spec.IntRange(Int.MIN_VALUE, Int.MAX_VALUE),
        "ui_col_progress" to Spec.IntRange(Int.MIN_VALUE, Int.MAX_VALUE),
        "ui_col_progress_bg" to Spec.IntRange(Int.MIN_VALUE, Int.MAX_VALUE),
        "ui_col_list" to Spec.IntRange(Int.MIN_VALUE, Int.MAX_VALUE),
        // Comma list of panel row keys (resource names). Unknown keys are harmless: the panel ignores keys it does not have.
        "quick_panel_hidden" to Spec.Text(600) { s -> s.all { it in 'a'..'z' || it in '0'..'9' || it == '_' || it == ',' } },
        "theme_color" to Spec.IntRange(0, 4, 1),
        "theme_mode" to Spec.IntRange(0, 2, 1),
        "download_path" to Spec.Text(512) { isPlausiblePath(it) },
        "torrent_encryption" to Spec.IntRange(0, 2),
        "top_window_enabled" to Spec.Bool,
        "top_window_stats" to Spec.IntRange(0, 63),
        "top_window_text_color" to Spec.IntRange(Int.MIN_VALUE, Int.MAX_VALUE),
        "top_window_bg_color" to Spec.IntRange(Int.MIN_VALUE, Int.MAX_VALUE),
        "top_window_bar_color" to Spec.IntRange(Int.MIN_VALUE, Int.MAX_VALUE),
        "top_window_opacity" to Spec.IntRange(10, 100),
        "top_window_width" to Spec.IntRange(0, 20000),
        "top_window_line_height" to Spec.IntRange(1, 128),
        "top_window_text_size" to Spec.IntRange(1, 32),
        "top_window_h_align" to Spec.IntRange(0, 2),
        "top_window_v_align" to Spec.IntRange(0, 2),
        "top_window_offset_x" to Spec.IntRange(0, 20000),
        "top_window_offset_y" to Spec.IntRange(0, 20000)
    )

    /** Empty means "not set". Otherwise an absolute path without ".." parts, NUL or line breaks. */
    fun isPlausiblePath(p: String): Boolean {
        if (p.isEmpty()) return true
        if (!p.startsWith("/")) return false
        if (p.any { it == '\u0000' || it == '\n' || it == '\r' }) return false
        return p.split('/').none { it == ".." }
    }

    /** Writes the known settings found in [values] whose stored type matches. Others are left out. */
    fun format(values: Map<String, Any?>): String {
        val sb = StringBuilder(MAGIC).append('\n')
        for ((key, spec) in specs) {
            val v = values[key] ?: continue
            val text = when {
                spec is Spec.Bool && v is Boolean -> v.toString()
                spec is Spec.IntRange && v is Int -> v.toString()
                spec is Spec.LongRange && v is Long -> v.toString()
                spec is Spec.Text && v is String -> escape(v)
                else -> continue
            }
            sb.append(key).append('=').append(text).append('\n')
        }
        return sb.toString()
    }

    /**
     * [isSettingsFile] is false when the first line is not the version line (nothing is accepted then).
     * [ignored]: lines with keys we do not know. [invalid]: known keys whose value is wrong or out of range.
     */
    data class Result(val isSettingsFile: Boolean, val accepted: Map<String, Any>, val ignored: Int, val invalid: Int)

    fun parse(text: String): Result {
        val lines = text.removePrefix("\uFEFF").lines().map { it.trimEnd('\r') }
        val first = lines.firstOrNull { it.isNotBlank() }?.trim()
        if (first != MAGIC) return Result(false, emptyMap(), 0, 0)
        val accepted = LinkedHashMap<String, Any>()
        var ignored = 0
        var invalid = 0
        for (line in lines.dropWhile { it.isBlank() }.drop(1)) {
            if (line.isBlank() || line.startsWith("#")) continue
            val i = line.indexOf('=')
            if (i <= 0) { invalid++; continue }
            val key = line.substring(0, i).trim()
            val spec = specs[key]
            if (spec == null) { ignored++; continue }
            val value = decode(spec, line.substring(i + 1))
            if (value == null) invalid++ else accepted[key] = value
        }
        return Result(true, accepted, ignored, invalid)
    }

    private fun decode(spec: Spec, raw: String): Any? = when (spec) {
        is Spec.Bool -> when (raw.trim()) { "true" -> true; "false" -> false; else -> null }
        is Spec.IntRange -> raw.trim().toIntOrNull()?.takeIf { (spec.zeroOk && it == 0) || (it in spec.min..spec.max && (it - spec.min) % spec.step == 0) }
        is Spec.LongRange -> raw.trim().toLongOrNull()?.takeIf { it in spec.min..spec.max && (it - spec.min) % spec.step == 0L }
        is Spec.Text -> unescape(raw)?.takeIf { it.length <= spec.maxLength && spec.check(it) }
    }

    private fun escape(s: String) = s.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r")

    private fun unescape(s: String): String? {
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c != '\\') { sb.append(c); i++; continue }
            if (i + 1 >= s.length) return null
            when (s[i + 1]) {
                '\\' -> sb.append('\\')
                'n' -> sb.append('\n')
                'r' -> sb.append('\r')
                else -> return null
            }
            i += 2
        }
        return sb.toString()
    }
}
