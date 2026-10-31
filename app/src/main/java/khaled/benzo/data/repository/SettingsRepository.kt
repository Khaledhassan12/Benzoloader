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

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    var defaultDownloadPath: String
        get() = prefs.getString(KEY_DOWNLOAD_PATH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DOWNLOAD_PATH, value).apply()

    companion object {
        private const val KEY_MAX_CONCURRENT = "max_concurrent"
        private const val KEY_DEFAULT_THREADS = "default_threads"
        private const val KEY_WIFI_ONLY = "wifi_only"
        private const val KEY_SPEED_LIMIT = "speed_limit"
        private const val KEY_BATTERY_THRESHOLD = "battery_threshold"
        private const val KEY_STOP_ON_UNPLUG = "stop_on_unplug"
        private const val KEY_SOUND_COMPLETE = "sound_complete"
        private const val KEY_VIBRATE_COMPLETE = "vibrate_complete"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_DOWNLOAD_PATH = "download_path"
    }
}
