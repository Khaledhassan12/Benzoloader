package khaled.benzo.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import khaled.benzo.data.repository.SettingsRepository

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    val repository = SettingsRepository(application)

    fun saveSettings(
        maxDownloads: Int,
        defaultThreads: Int,
        wifiOnly: Boolean,
        batteryThreshold: Int,
        stopOnUnplug: Boolean,
        soundComplete: Boolean,
        vibrateComplete: Boolean,
        speedLimitKBps: Int,
        scheduleEnabled: Boolean,
        scheduleStartMinute: Int,
        scheduleStopMinute: Int,
        catalogEnabled: Boolean,
        errorRetries: Int,
        pauseAfterError: Boolean
    ) {
        repository.maxConcurrentDownloads = maxDownloads
        repository.defaultPartsCount = defaultThreads
        repository.isWifiOnly = wifiOnly
        repository.batteryThresholdPercent = batteryThreshold
        repository.stopOnChargerUnplug = stopOnUnplug
        repository.soundOnComplete = soundComplete
        repository.vibrateOnComplete = vibrateComplete
        repository.speedLimitKBps = speedLimitKBps
        repository.scheduleEnabled = scheduleEnabled
        repository.scheduleStartMinute = scheduleStartMinute
        repository.scheduleStopMinute = scheduleStopMinute
        repository.catalogEnabled = catalogEnabled
        repository.errorRetries = errorRetries
        repository.pauseAfterError = pauseAfterError
        khaled.benzo.service.ScheduleManager.rearm(getApplication())
    }
}
