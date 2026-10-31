package khaled.benzo.ui.torrent

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import khaled.benzo.data.model.TorrentFileEntry
import khaled.benzo.databinding.ItemTorrentFileBinding
import java.util.Locale

class TorrentFileAdapter(
    private val files: List<TorrentFileEntry>
) : RecyclerView.Adapter<TorrentFileAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTorrentFileBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(files[position])
    }

    override fun getItemCount(): Int = files.size

    inner class ViewHolder(private val binding: ItemTorrentFileBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: TorrentFileEntry) {
            binding.txtTorrentFileName.text = entry.path
            binding.txtTorrentFileSize.text = formatBytes(entry.size)
            binding.chkFileSelected.isChecked = entry.isSelected

            binding.chkFileSelected.setOnCheckedChangeListener { _, isChecked ->
                entry.isSelected = isChecked
                entry.priority = if (isChecked) 1 else 0
            }
        }

        private fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
            return String.format(
                Locale.US,
                "%.1f %s",
                bytes / Math.pow(1024.0, digitGroups.toDouble()),
                units[digitGroups]
            )
        }
    }
}
