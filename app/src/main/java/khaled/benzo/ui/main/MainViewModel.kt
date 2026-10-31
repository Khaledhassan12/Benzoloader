package khaled.benzo.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import khaled.benzo.data.model.DownloadItem
import khaled.benzo.data.model.FileCategory
import khaled.benzo.data.repository.DownloadRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DownloadRepository(application)
    private val _selectedCategory = MutableStateFlow<FileCategory?>(FileCategory.ALL)

    val downloads: StateFlow<List<DownloadItem>> = MutableStateFlow<List<DownloadItem>>(emptyList())

    val selectedCategory = _selectedCategory.asStateFlow()

    init {
        viewModelScope.launch {
            _selectedCategory.flatMapLatest { category ->
                repository.getAllDownloads(category)
            }.collect { list ->
                (downloads as MutableStateFlow).value = list
            }
        }
    }

    fun setCategory(category: FileCategory?) {
        _selectedCategory.value = category
    }

    fun deleteDownload(id: Long) {
        viewModelScope.launch {
            repository.deleteDownload(id)
        }
    }

    fun pauseAll() {
        viewModelScope.launch {
            repository.pauseAll()
        }
    }

    fun resumeAll() {
        viewModelScope.launch {
            repository.resumeAll()
        }
    }
}
