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
        vibrateComplete: Boolean
    ) {
        repository.maxConcurrentDownloads = maxDownloads
        repository.defaultPartsCount = defaultThreads
        repository.isWifiOnly = wifiOnly
        repository.batteryThresholdPercent = batteryThreshold
        repository.pauseOnBatteryLow = stopOnUnplug
        repository.soundOnComplete = soundComplete
        repository.vibrateOnComplete = vibrateComplete
    }
}
