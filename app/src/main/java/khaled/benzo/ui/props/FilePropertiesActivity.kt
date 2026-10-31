package khaled.benzo.ui.props

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import khaled.benzo.data.repository.DownloadRepository
import khaled.benzo.databinding.ActivityFilePropertiesBinding
import kotlinx.coroutines.launch

class FilePropertiesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFilePropertiesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFilePropertiesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val downloadId = intent.getLongExtra("download_id", -1L)
        if (downloadId != -1L) {
            val repository = DownloadRepository(this)
            lifecycleScope.launch {
                val item = repository.getDownload(downloadId)
                if (item != null) {
                    binding.txtPropFileName.text = item.fileName
                    binding.txtPropUrl.text = "URL: ${item.url}"
                    binding.txtPropPath.text = "Path: ${item.savePath}/${item.fileName}"
                    binding.txtPropSize.text = "Size: ${item.downloadedBytes} / ${if (item.totalBytes > 0) item.totalBytes else "Unknown"} bytes"
                    binding.txtPropParts.text = "Threads: ${item.parts}"
                }
            }
        }

        binding.btnCloseProperties.setOnClickListener {
            finish()
        }
    }
}
