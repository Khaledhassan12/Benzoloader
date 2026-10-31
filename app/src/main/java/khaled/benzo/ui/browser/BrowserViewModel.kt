package khaled.benzo.ui.browser

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import khaled.benzo.core.sniffer.DetectedMedia
import khaled.benzo.core.sniffer.MediaSniffer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val _detectedMediaList = MutableStateFlow<List<DetectedMedia>>(emptyList())
    val detectedMediaList: StateFlow<List<DetectedMedia>> = _detectedMediaList.asStateFlow()

    val mediaSniffer = MediaSniffer { media: DetectedMedia ->
        val current = _detectedMediaList.value.toMutableList()
        if (current.none { it.url == media.url }) {
            current.add(media)
            _detectedMediaList.value = current
        }
    }

    fun clearDetectedMedia() {
        _detectedMediaList.value = emptyList()
    }
}
