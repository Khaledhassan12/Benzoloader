package khaled.benzo.ui.settings

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import khaled.benzo.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbarSettings.setNavigationOnClickListener {
            saveAndFinish()
        }

        loadCurrentSettings()
    }

    private fun loadCurrentSettings() {
        val repo = viewModel.repository
        binding.sliderMaxDownloads.value = repo.maxConcurrentDownloads.toFloat()
        binding.sliderDefaultThreads.value = repo.defaultPartsCount.toFloat()
        binding.switchWifiOnly.isChecked = repo.isWifiOnly
        binding.sliderBattery.value = repo.batteryThresholdPercent.toFloat()
        binding.switchStopOnUnplug.isChecked = repo.pauseOnBatteryLow
        binding.switchSoundComplete.isChecked = repo.soundOnComplete
        binding.switchVibrateComplete.isChecked = repo.vibrateOnComplete
    }

    private fun saveAndFinish() {
        viewModel.saveSettings(
            maxDownloads = binding.sliderMaxDownloads.value.toInt(),
            defaultThreads = binding.sliderDefaultThreads.value.toInt(),
            wifiOnly = binding.switchWifiOnly.isChecked,
            batteryThreshold = binding.sliderBattery.value.toInt(),
            stopOnUnplug = binding.switchStopOnUnplug.isChecked,
            soundComplete = binding.switchSoundComplete.isChecked,
            vibrateComplete = binding.switchVibrateComplete.isChecked
        )
        finish()
    }

    override fun onBackPressed() {
        saveAndFinish()
    }
}
