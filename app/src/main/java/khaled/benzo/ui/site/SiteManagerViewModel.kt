package khaled.benzo.ui.site

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import khaled.benzo.data.db.BenzoDatabase
import khaled.benzo.data.db.SiteEntity
import khaled.benzo.data.model.SiteProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SiteManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val siteDao = BenzoDatabase.getInstance(application).siteDao()

    private val _sites = MutableStateFlow<List<SiteProfile>>(emptyList())
    val sites: StateFlow<List<SiteProfile>> = _sites.asStateFlow()

    fun loadSites() {
        viewModelScope.launch {
            val list = siteDao.getAllSites()
            _sites.value = list.map { it.toModel() }
        }
    }

    fun addSite(domain: String, username: String, pass: String) {
        viewModelScope.launch {
            siteDao.insertSite(SiteEntity(domain = domain, username = username, password = pass))
            loadSites()
        }
    }

    fun deleteSite(domain: String) {
        viewModelScope.launch {
            siteDao.deleteByDomain(domain)
            loadSites()
        }
    }
}
