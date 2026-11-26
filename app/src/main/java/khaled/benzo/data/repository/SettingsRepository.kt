package khaled.benzo.data.repository

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("benzo_preferences", Context.MODE_PRIVATE)

    var maxConcurrentDownloads: Int
        get() = prefs.getInt(KEY_MAX_CONCURRENT, 3)
        set(value) = prefs.edit().putInt(KEY_MAX_CONCURRENT, value).apply()

    var defaultThreadsCount: Int
        get() = prefs.getInt(KEY_DEFAULT_THREADS, 4)
        set(value) = prefs.edit().putInt(KEY_DEFAULT_THREADS, value).apply()

    var defaultPartsCount: Int
        get() = defaultThreadsCount
        set(value) { defaultThreadsCount = value }

    var pauseOnBatteryLow: Boolean
        get() = batteryThresholdPercent > 0
        set(value) {
            if (!value) batteryThresholdPercent = 0
            else if (batteryThresholdPercent == 0) batteryThresholdPercent = 15
        }

    var isWifiOnly: Boolean
        get() = prefs.getBoolean(KEY_WIFI_ONLY, false)
        set(value) = prefs.edit().putBoolean(KEY_WIFI_ONLY, value).apply()

    var speedLimitBytesPerSec: Long
        get() = prefs.getLong(KEY_SPEED_LIMIT, 0L) // 0 means unlimited
        set(value) = prefs.edit().putLong(KEY_SPEED_LIMIT, value).apply()

    var batteryThresholdPercent: Int
        get() = prefs.getInt(KEY_BATTERY_THRESHOLD, 15)
        set(value) = prefs.edit().putInt(KEY_BATTERY_THRESHOLD, value).apply()

    var stopOnChargerUnplug: Boolean
        get() = prefs.getBoolean(KEY_STOP_ON_UNPLUG, false)
        set(value) = prefs.edit().putBoolean(KEY_STOP_ON_UNPLUG, value).apply()

    var soundOnComplete: Boolean
        get() = prefs.getBoolean(KEY_SOUND_COMPLETE, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_COMPLETE, value).apply()

    var vibrateOnComplete: Boolean
        get() = prefs.getBoolean(KEY_VIBRATE_COMPLETE, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATE_COMPLETE, value).apply()

    var scheduleEnabled: Boolean
        get() = prefs.getBoolean(KEY_SCHEDULE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SCHEDULE_ENABLED, value).apply()

    /** Minutes since midnight. */
    var scheduleStartMinute: Int
        get() = prefs.getInt(KEY_SCHEDULE_START, 0)
        set(value) = prefs.edit().putInt(KEY_SCHEDULE_START, value).apply()

    var scheduleStopMinute: Int
        get() = prefs.getInt(KEY_SCHEDULE_STOP, 0)
        set(value) = prefs.edit().putInt(KEY_SCHEDULE_STOP, value).apply()

    /** Speed limit in KB/s for the settings screen; 0 = unlimited. Stored as bytes per second. */
    var speedLimitKBps: Int
        get() = (speedLimitBytesPerSec / 1024L).toInt()
        set(value) { speedLimitBytesPerSec = value.coerceAtLeast(0) * 1024L }

    /** Sort new downloads into Video/Music/Documents/... folders under the download folder. Off by default. */
    var catalogFolderNames: khaled.benzo.core.catalog.FolderNames
        get() = khaled.benzo.core.catalog.FolderNames.decode(prefs.getString(KEY_CATALOG_NAMES, null))
        set(value) = prefs.edit().putString(KEY_CATALOG_NAMES, value.encode()).apply()

    var catalogEnabled: Boolean
        get() = prefs.getBoolean(KEY_CATALOG, false)
        set(value) = prefs.edit().putBoolean(KEY_CATALOG, value).apply()

    /** How many times a failing part is retried (with backoff) before the download is marked as failed. */
    var errorRetries: Int
        get() = prefs.getInt(KEY_ERROR_RETRIES, 4).coerceIn(0, 10)
        set(value) = prefs.edit().putInt(KEY_ERROR_RETRIES, value.coerceIn(0, 10)).apply()

    /** A download that fails after all retries becomes Paused (keeps its partial file) instead of Failed. */
    var pauseAfterError: Boolean
        get() = prefs.getBoolean(KEY_PAUSE_AFTER_ERROR, false)
        set(value) = prefs.edit().putBoolean(KEY_PAUSE_AFTER_ERROR, value).apply()

    /** A download that failed for a reason that may pass (dropped connection) is queued again by itself, a few times. Off by default: opt-in, so existing installs keep their behaviour. */
    var autoResume: Boolean
        get() = prefs.getBoolean(KEY_AUTO_RESUME, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_RESUME, value).apply()

    /** Show the ongoing notification's content on the lock screen. Off by default: the system's private behaviour stays. */
    var notifOnLockScreen: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_LOCK, false)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_LOCK, value).apply()

    /** Add a "Pause all" button to the ongoing notification (Exit is always there). Off by default. */
    var notifPauseAllButton: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_PAUSE_ALL, false)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_PAUSE_ALL, value).apply()

    /** Saved comma list of notification command buttons; null until the user opens the picker (then the older Pause all switch applies). */
    var notifButtons: String?
        get() = prefs.getString("notif_buttons", null)
        set(value) = prefs.edit().putString("notif_buttons", value ?: "").apply()

    /** Keep the screen on while the app is open. Off by default. */
    var keepScreenOn: Boolean
        get() = prefs.getBoolean(KEY_KEEP_SCREEN, false)
        set(value) = prefs.edit().putBoolean(KEY_KEEP_SCREEN, value).apply()

    /** Remove finished downloads from the list (not the files) when the app starts. Off by default. */
    var clearFinishedOnStart: Boolean
        get() = prefs.getBoolean(KEY_CLEAR_FINISHED, false)
        set(value) = prefs.edit().putBoolean(KEY_CLEAR_FINISHED, value).apply()

    /** Download torrent pieces in order. Off by default. Applied when a torrent starts or resumes in this app. */
    var torrentSequential: Boolean
        get() = prefs.getBoolean(KEY_TORRENT_SEQ, false)
        set(value) = prefs.edit().putBoolean(KEY_TORRENT_SEQ, value).apply()

    /** Keep uploading a finished torrent. On by default: the app never stopped seeding before this setting existed. */
    var torrentSeedAfterFinish: Boolean
        get() = prefs.getBoolean(KEY_TORRENT_SEED, true)
        set(value) = prefs.edit().putBoolean(KEY_TORRENT_SEED, value).apply()

    /** Stop uploading a finished torrent at this share ratio, in tenths (15 = 1.5). 0 = no limit (the default: seeding never stopped before). */
    var seedRatioTenths: Int
        get() = prefs.getInt(KEY_SEED_RATIO, 0).coerceIn(0, 100)
        set(value) = prefs.edit().putInt(KEY_SEED_RATIO, value.coerceIn(0, 100)).apply()

    /** Stop uploading after this many minutes of seeding. 0 = no limit. */
    var torrentDht: Boolean
        get() = prefs.getBoolean(KEY_TORRENT_DHT, true)
        set(value) = prefs.edit().putBoolean(KEY_TORRENT_DHT, value).apply()

    /** 0 = the library's own limit. */
    var torrentConnections: Int
        get() = prefs.getInt(KEY_TORRENT_CONNECTIONS, 0).coerceIn(0, 1000)
        set(value) = prefs.edit().putInt(KEY_TORRENT_CONNECTIONS, value.coerceIn(0, 1000)).apply()

    /** Fixed torrent listen port. Off keeps the library's own listen setting. */
    var torrentFixedPort: Boolean
        get() = prefs.getBoolean(KEY_TORRENT_FIXED_PORT, false)
        set(value) = prefs.edit().putBoolean(KEY_TORRENT_FIXED_PORT, value).apply()

    var torrentPort: Int
        get() = prefs.getInt(KEY_TORRENT_PORT, 6881).coerceIn(1024, 65535)
        set(value) = prefs.edit().putInt(KEY_TORRENT_PORT, value.coerceIn(1024, 65535)).apply()

    var torrentAnonymous: Boolean
        get() = prefs.getBoolean(KEY_TORRENT_ANON, false)
        set(value) = prefs.edit().putBoolean(KEY_TORRENT_ANON, value).apply()

    /** Start order of waiting downloads: false = oldest first (new ones go to the bottom of the queue), true = newest first. */
    /** Smallest part a download is split into, in KB. 512 is the value the engine always used, so default behaviour is unchanged. */
    var vibrateSuccessMs: Int
        get() = prefs.getInt(KEY_VIB_SUCCESS, 150).coerceIn(50, 1000)
        set(value) = prefs.edit().putInt(KEY_VIB_SUCCESS, value.coerceIn(50, 1000)).apply()

    /** Separate from the completion switch: a failure vibrates only when the user turns this on. Default OFF. */
    /** Remove finished-download notifications when the user taps Exit. Off: they stay until swiped. */
    var clearNotifsOnExit: Boolean
        get() = prefs.getBoolean(KEY_CLEAR_NOTIFS_EXIT, false)
        set(value) = prefs.edit().putBoolean(KEY_CLEAR_NOTIFS_EXIT, value).apply()

    var vibrateOnError: Boolean
        get() = prefs.getBoolean(KEY_VIB_ERROR_ON, false)
        set(value) = prefs.edit().putBoolean(KEY_VIB_ERROR_ON, value).apply()

    /** Length of the buzz when the last waiting download finishes. 500 ms is the CAPTURED reference value, not a verified factory default. */
    var vibrateTotalMs: Int
        get() = prefs.getInt("vib_total_ms", 500).coerceIn(50, 1000)
        set(value) = prefs.edit().putInt("vib_total_ms", value.coerceIn(50, 1000)).apply()

    var vibrateErrorMs: Int
        get() = prefs.getInt(KEY_VIB_ERROR, 300).coerceIn(50, 1000)
        set(value) = prefs.edit().putInt(KEY_VIB_ERROR, value.coerceIn(50, 1000)).apply()

    var minThreadKb: Int
        get() = prefs.getInt(KEY_MIN_THREAD_KB, 512).coerceIn(128, 4096)
        set(value) = prefs.edit().putInt(KEY_MIN_THREAD_KB, value.coerceIn(128, 4096)).apply()

    /** User-agent sent when a download has none of its own. Blank means the HTTP client's own default. */
    var defaultUserAgent: String
        get() = prefs.getString(KEY_DEFAULT_UA, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_DEFAULT_UA, value.trim().take(300)).apply()

    /** Proxies the user entered, one per line (always valid: only parsed values are stored). Not part of the settings export. */
    var proxyList: List<String>
        get() = prefs.getString(KEY_PROXY_LIST, "").orEmpty().split('\n').filter { it.isNotBlank() }
        set(value) = prefs.edit().putString(KEY_PROXY_LIST, value.distinct().take(20).joinToString("\n")).apply()

    /** The chosen proxy's label, or blank for "disabled". */
    var proxySelected: String
        get() = prefs.getString(KEY_PROXY_SELECTED, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_PROXY_SELECTED, value).apply()

    fun activeProxy(): khaled.benzo.core.net.ProxyConfig? =
        (khaled.benzo.core.net.ProxyConfig.parse(proxySelected) as? khaled.benzo.core.net.ProxyConfig.Parsed.Ok)?.config

    /** Where new downloads appear in the list: false = top (newest first, the long-standing order), true = bottom. Display only; start order is [newestFirst]. */
    var listAddToBottom: Boolean
        get() = prefs.getBoolean(KEY_LIST_BOTTOM, false)
        set(value) = prefs.edit().putBoolean(KEY_LIST_BOTTOM, value).apply()

    /** Links opened or shared from other apps are added at once, without showing the editor. Off by default: any app can send a link. */
    var quickAddLinks: Boolean
        get() = prefs.getBoolean(KEY_QUICK_ADD, false)
        set(value) = prefs.edit().putBoolean(KEY_QUICK_ADD, value).apply()

    /** Only meaningful with [quickAddLinks]: start the download after the quick add. Off: it is added to the queue and the queue rules decide. */
    var quickAddStart: Boolean
        get() = prefs.getBoolean(KEY_QUICK_ADD_START, false)
        set(value) = prefs.edit().putBoolean(KEY_QUICK_ADD_START, value).apply()

    /** [khaled.benzo.core.queue.AfterFinish] code: what the service does when the last download completes. */
    var afterFinish: Int
        get() = prefs.getInt(KEY_AFTER_FINISH, 0).coerceIn(0, 1)
        set(value) = prefs.edit().putInt(KEY_AFTER_FINISH, value.coerceIn(0, 1)).apply()

    var newestFirst: Boolean
        get() = prefs.getBoolean(KEY_NEWEST_FIRST, false)
        set(value) = prefs.edit().putBoolean(KEY_NEWEST_FIRST, value).apply()

    var seedMinutes: Int
        get() = prefs.getInt(KEY_SEED_MINUTES, 0).coerceIn(0, 4320)
        set(value) = prefs.edit().putInt(KEY_SEED_MINUTES, value.coerceIn(0, 4320)).apply()

    /** Remember visited pages in the browser. Off by default (privacy). */
    var browserHistory: Boolean
        get() = prefs.getBoolean(KEY_BROWSER_HISTORY, false)
        set(value) = prefs.edit().putBoolean(KEY_BROWSER_HISTORY, value).apply()

    /** Keep the browser's open tabs (web addresses and titles) on this device and reopen them next time. Off by default (privacy). */
    var browserSaveTabs: Boolean
        get() = prefs.getBoolean(KEY_BROWSER_SAVE_TABS, false)
        set(value) = prefs.edit().putBoolean(KEY_BROWSER_SAVE_TABS, value).also { if (!value) it.remove(KEY_BROWSER_TABS) }.apply()

    /** Output of TabList.serialize(). Not part of the settings backup. */
    var browserSavedTabs: String?
        get() = prefs.getString(KEY_BROWSER_TABS, null)
        set(value) = (if (value.isNullOrEmpty()) prefs.edit().remove(KEY_BROWSER_TABS) else prefs.edit().putString(KEY_BROWSER_TABS, value)).apply()

    /** The user said no to the optional all-files access request. Not part of the settings backup. Settings still offers it. */
    var allFilesDeclined: Boolean
        get() = prefs.getBoolean(KEY_ALLFILES_DECLINED, false)
        set(value) = prefs.edit().putBoolean(KEY_ALLFILES_DECLINED, value).apply()

    /** Bottom navigation bar on the main screen. Shown by default on a fresh install (user instruction, 25 Nov 16:23). */
    /** Interface: swap the two side menus (navigation drawer on the end edge, filter panel on the start edge). Default off, the existing layout. */
    var swapSideMenus: Boolean
        get() = prefs.getBoolean(KEY_SWAP_MENUS, false)
        set(value) = prefs.edit().putBoolean(KEY_SWAP_MENUS, value).apply()

    /** Speed graph at the top of the quick panel. Default shown; the default is my choice, the reference default was not seen. */
    var quickGraphVisible: Boolean
        get() = prefs.getBoolean("quick_graph_visible", true)
        set(value) = prefs.edit().putBoolean("quick_graph_visible", value).apply()

    /** "Create empty file": reserve the whole file before downloading. Default ON is the CAPTURED reference value, not a verified factory default. In the settings backup. */
    var createEmptyFile: Boolean
        get() = prefs.getBoolean("create_empty_file", true)
        set(value) = prefs.edit().putBoolean("create_empty_file", value).apply()

    /** Quick-panel rows hidden with the eye editor (string keys). */
    var quickPanelHidden: Set<String>
        get() = (prefs.getString("quick_panel_hidden", "") ?: "").split(',').filter { it.isNotEmpty() }.toSet()
        set(value) = prefs.edit().putString("quick_panel_hidden", value.sorted().joinToString(",")).apply()

    var bottomBar: Boolean
        get() = prefs.getBoolean(KEY_BOTTOM_BAR, true)
        set(value) = prefs.edit().putBoolean(KEY_BOTTOM_BAR, value).apply()

    /** 0 = device colours (Material You, Android 12+), 1 = green (Benzo default), 2 blue, 3 purple, 4 orange. */
    /** 0 follow the system, 1 light, 2 dark. Separate from the old unread dark_mode key; a new install and an upgrade both start on 0 (follow the system, which is what the app did before). */
    var themeMode: Int
        get() = prefs.getInt(KEY_THEME_MODE, 0).coerceIn(0, 2)
        set(value) = prefs.edit().putInt(KEY_THEME_MODE, value.coerceIn(0, 2)).apply()

    var themeColor: Int
        get() = prefs.getInt(KEY_THEME_COLOR, 0)
        set(value) = prefs.edit().putInt(KEY_THEME_COLOR, value).apply()

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    var defaultDownloadPath: String
        get() = prefs.getString(KEY_DOWNLOAD_PATH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DOWNLOAD_PATH, value).apply()

    /**
     * Puts every setting the backup knows back to its default by removing exactly those keys.
     * Also drops the saved tab list, because "keep open tabs" goes back to off and the setter does the same when it is switched off.
     * Everything else in the file stays: the "all files access declined" choice. Downloads, history, bookmarks and credentials live elsewhere.
     */
    fun resetToDefaults() {
        val e = prefs.edit()
        backupValues().keys.forEach { e.remove(it) }
        e.remove(KEY_BROWSER_TABS)
        e.apply()
    }

    /** Current effective value of every setting that the settings backup knows (see SettingsBackup.specs). */
    fun backupValues(): Map<String, Any?> = mapOf(
        KEY_MAX_CONCURRENT to maxConcurrentDownloads, KEY_DEFAULT_THREADS to defaultThreadsCount,
        KEY_WIFI_ONLY to isWifiOnly, KEY_SPEED_LIMIT to speedLimitBytesPerSec,
        KEY_BATTERY_THRESHOLD to batteryThresholdPercent, KEY_STOP_ON_UNPLUG to stopOnChargerUnplug,
        KEY_SOUND_COMPLETE to soundOnComplete, KEY_VIBRATE_COMPLETE to vibrateOnComplete,
        KEY_SCHEDULE_ENABLED to scheduleEnabled, KEY_SCHEDULE_START to scheduleStartMinute,
        KEY_SCHEDULE_STOP to scheduleStopMinute, KEY_CATALOG to catalogEnabled,
        KEY_CATALOG_NAMES to catalogFolderNames.encode(), KEY_ERROR_RETRIES to errorRetries,
        KEY_PAUSE_AFTER_ERROR to pauseAfterError, KEY_AUTO_RESUME to autoResume,
        KEY_NOTIF_LOCK to notifOnLockScreen, KEY_NOTIF_PAUSE_ALL to notifPauseAllButton,
        KEY_KEEP_SCREEN to keepScreenOn, KEY_CLEAR_FINISHED to clearFinishedOnStart,
        KEY_TORRENT_SEQ to torrentSequential, KEY_TORRENT_SEED to torrentSeedAfterFinish, KEY_SEED_RATIO to seedRatioTenths, KEY_SEED_MINUTES to seedMinutes, KEY_TORRENT_DHT to torrentDht, KEY_TORRENT_CONNECTIONS to torrentConnections, KEY_NEWEST_FIRST to newestFirst, KEY_AFTER_FINISH to afterFinish, KEY_QUICK_ADD to quickAddLinks, KEY_QUICK_ADD_START to quickAddStart, KEY_LIST_BOTTOM to listAddToBottom, KEY_SWAP_MENUS to swapSideMenus, KEY_MIN_THREAD_KB to minThreadKb, KEY_VIB_SUCCESS to vibrateSuccessMs, KEY_VIB_ERROR to vibrateErrorMs, KEY_VIB_ERROR_ON to vibrateOnError, KEY_CLEAR_NOTIFS_EXIT to clearNotifsOnExit, KEY_DEFAULT_UA to defaultUserAgent, KEY_TORRENT_FIXED_PORT to torrentFixedPort, KEY_TORRENT_PORT to torrentPort, KEY_TORRENT_ANON to torrentAnonymous,
        KEY_BROWSER_HISTORY to browserHistory, KEY_BROWSER_SAVE_TABS to browserSaveTabs, KEY_DARK_MODE to isDarkMode, KEY_BOTTOM_BAR to bottomBar, KEY_THEME_COLOR to themeColor, KEY_THEME_MODE to themeMode,
        KEY_DOWNLOAD_PATH to defaultDownloadPath,
        "torrent_encryption" to torrentEncryption,
        "vib_total_ms" to vibrateTotalMs, "notif_buttons" to notifButtons, "create_empty_file" to createEmptyFile, "quick_graph_visible" to quickGraphVisible,
        "quick_panel_hidden" to quickPanelHidden.sorted().joinToString(","),
        "top_window_enabled" to topWindowEnabled, "top_window_stats" to topWindowStatsMask,
        "top_window_text_color" to topWindowTextColor, "top_window_bg_color" to topWindowBackgroundColor,
        "top_window_bar_color" to topWindowBarColor, "top_window_opacity" to topWindowOpacityPercent,
        "top_window_width" to topWindowWidthPx, "top_window_line_height" to topWindowLineHeightPx,
        "top_window_text_size" to topWindowTextSizePx, "top_window_h_align" to topWindowHAlign,
        "top_window_v_align" to topWindowVAlign, "top_window_offset_x" to topWindowOffsetX,
        "top_window_offset_y" to topWindowOffsetY
    )

    /**
     * Writes already validated backup values in one commit. The download folder is applied only when
     * [folderUsable] accepts it (it may not exist on a new install); returns true when it was left unchanged for that reason.
     */
    fun applyBackup(values: Map<String, Any>, folderUsable: (String) -> Boolean): Boolean {
        val e = prefs.edit()
        var pathSkipped = false
        for ((key, v) in values) {
            when (v) {
                is Boolean -> e.putBoolean(key, v)
                is Int -> e.putInt(key, v)
                is Long -> e.putLong(key, v)
                is String -> if (key == KEY_DOWNLOAD_PATH && v.isNotEmpty() && !folderUsable(v)) pathSkipped = true
                             else e.putString(key, v)
            }
        }
        e.apply()
        return pathSkipped
    }

    /** Torrent encryption mode index: 0 prefer (default), 1 require, 2 disable. See TorrentEncryption. */
    var torrentEncryption: Int
        get() = prefs.getInt("torrent_encryption", 0).coerceIn(0, 2)
        set(v) = prefs.edit().putInt("torrent_encryption", v.coerceIn(0, 2)).apply()

    // Top window (overlay). Defaults: OFF, and the values seen in the reference captures where they are device independent.
    var topWindowEnabled: Boolean
        get() = prefs.getBoolean("top_window_enabled", false)
        set(v) = prefs.edit().putBoolean("top_window_enabled", v).apply()
    var topWindowStatsMask: Int
        get() = prefs.getInt("top_window_stats", 0b001001) // percent + speed
        set(v) = prefs.edit().putInt("top_window_stats", v and 0b111111).apply()
    var topWindowTextColor: Int
        get() = prefs.getInt("top_window_text_color", 0xFFFFFFFF.toInt())
        set(v) = prefs.edit().putInt("top_window_text_color", v).apply()
    var topWindowBackgroundColor: Int
        get() = prefs.getInt("top_window_bg_color", 0xFF000000.toInt())
        set(v) = prefs.edit().putInt("top_window_bg_color", v).apply()
    var topWindowBarColor: Int
        get() = prefs.getInt("top_window_bar_color", 0xFFE53935.toInt())
        set(v) = prefs.edit().putInt("top_window_bar_color", v).apply()
    var topWindowOpacityPercent: Int
        get() = prefs.getInt("top_window_opacity", 60)
        set(v) = prefs.edit().putInt("top_window_opacity", v.coerceIn(10, 100)).apply()
    var topWindowWidthPx: Int
        get() = prefs.getInt("top_window_width", 0)
        set(v) = prefs.edit().putInt("top_window_width", v.coerceAtLeast(0)).apply()
    var topWindowLineHeightPx: Int
        get() = prefs.getInt("top_window_line_height", 41)
        set(v) = prefs.edit().putInt("top_window_line_height", v.coerceIn(1, 128)).apply()
    var topWindowTextSizePx: Int
        get() = prefs.getInt("top_window_text_size", 14)
        set(v) = prefs.edit().putInt("top_window_text_size", v.coerceIn(1, 32)).apply()
    var topWindowHAlign: Int
        get() = prefs.getInt("top_window_h_align", 0)
        set(v) = prefs.edit().putInt("top_window_h_align", v.coerceIn(0, 2)).apply()
    var topWindowVAlign: Int
        get() = prefs.getInt("top_window_v_align", 0)
        set(v) = prefs.edit().putInt("top_window_v_align", v.coerceIn(0, 2)).apply()
    var topWindowOffsetX: Int
        get() = prefs.getInt("top_window_offset_x", 0)
        set(v) = prefs.edit().putInt("top_window_offset_x", v.coerceAtLeast(0)).apply()
    var topWindowOffsetY: Int
        get() = prefs.getInt("top_window_offset_y", 0)
        set(v) = prefs.edit().putInt("top_window_offset_y", v.coerceAtLeast(0)).apply()

    fun topWindowConfig(): khaled.benzo.core.overlay.TopWindowConfig {
        val stats = khaled.benzo.core.overlay.TopStat.values().filter { topWindowStatsMask and (1 shl it.ordinal) != 0 }.toSet()
        return khaled.benzo.core.overlay.TopWindowConfig(
            enabled = topWindowEnabled, stats = stats, textColor = topWindowTextColor,
            backgroundColor = topWindowBackgroundColor, barColor = topWindowBarColor,
            opacityPercent = topWindowOpacityPercent, widthPx = topWindowWidthPx,
            lineHeightPx = topWindowLineHeightPx, textSizePx = topWindowTextSizePx,
            horizontal = khaled.benzo.core.overlay.TopWindowConfig.Align.values()[topWindowHAlign], vertical = khaled.benzo.core.overlay.TopWindowConfig.VAlign.values()[topWindowVAlign],
            offsetXPx = topWindowOffsetX, offsetYPx = topWindowOffsetY
        )
    }

    companion object {
        private const val KEY_MAX_CONCURRENT = "max_concurrent"
        private const val KEY_DEFAULT_THREADS = "default_threads"
        private const val KEY_WIFI_ONLY = "wifi_only"
        private const val KEY_SPEED_LIMIT = "speed_limit"
        private const val KEY_BATTERY_THRESHOLD = "battery_threshold"
        private const val KEY_STOP_ON_UNPLUG = "stop_on_unplug"
        private const val KEY_SOUND_COMPLETE = "sound_complete"
        private const val KEY_VIBRATE_COMPLETE = "vibrate_complete"
        private const val KEY_SCHEDULE_ENABLED = "schedule_enabled"
        private const val KEY_SCHEDULE_START = "schedule_start_minute"
        private const val KEY_SCHEDULE_STOP = "schedule_stop_minute"
        private const val KEY_CATALOG = "catalog_enabled"
        private const val KEY_CATALOG_NAMES = "catalog_folder_names"
        private const val KEY_ERROR_RETRIES = "error_retries"
        private const val KEY_PAUSE_AFTER_ERROR = "pause_after_error"
        private const val KEY_AUTO_RESUME = "auto_resume"
        private const val KEY_BROWSER_HISTORY = "browser_history"
        private const val KEY_BROWSER_SAVE_TABS = "browser_save_tabs"
        private const val KEY_BROWSER_TABS = "browser_saved_tabs"
        private const val KEY_TORRENT_SEQ = "torrent_sequential"
        private const val KEY_TORRENT_SEED = "torrent_seed_after_finish"
        private const val KEY_SEED_RATIO = "torrent_seed_ratio_tenths"
        private const val KEY_SEED_MINUTES = "torrent_seed_minutes"
        private const val KEY_TORRENT_DHT = "torrent_dht"
        private const val KEY_TORRENT_CONNECTIONS = "torrent_connections"
        private const val KEY_NEWEST_FIRST = "newest_first"
        private const val KEY_AFTER_FINISH = "after_finish"
        private const val KEY_QUICK_ADD = "quick_add_links"
        private const val KEY_QUICK_ADD_START = "quick_add_start"
        private const val KEY_LIST_BOTTOM = "list_add_to_bottom"
        private const val KEY_SWAP_MENUS = "swap_side_menus"
        private const val KEY_VIB_SUCCESS = "vibrate_success_ms"
        private const val KEY_VIB_ERROR = "vibrate_error_ms"
        private const val KEY_VIB_ERROR_ON = "vibrate_on_error"
        private const val KEY_CLEAR_NOTIFS_EXIT = "clear_notifs_on_exit"
        private const val KEY_MIN_THREAD_KB = "min_thread_kb"
        private const val KEY_DEFAULT_UA = "default_user_agent"
        private const val KEY_PROXY_LIST = "proxy_list"
        private const val KEY_PROXY_SELECTED = "proxy_selected"
        private const val KEY_TORRENT_FIXED_PORT = "torrent_fixed_port"
        private const val KEY_TORRENT_PORT = "torrent_port"
        private const val KEY_TORRENT_ANON = "torrent_anonymous"
        private const val KEY_ALLFILES_DECLINED = "allfiles_declined"
        private const val KEY_KEEP_SCREEN = "keep_screen_on"
        private const val KEY_CLEAR_FINISHED = "clear_finished_on_start"
        private const val KEY_NOTIF_LOCK = "notif_lock_screen"
        private const val KEY_NOTIF_PAUSE_ALL = "notif_pause_all_button"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_BOTTOM_BAR = "bottom_bar"
        private const val KEY_THEME_COLOR = "theme_color"
        private const val KEY_DOWNLOAD_PATH = "download_path"
    }
}
