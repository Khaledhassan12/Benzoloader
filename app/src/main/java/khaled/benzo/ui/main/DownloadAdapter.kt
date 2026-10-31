package khaled.benzo.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import khaled.benzo.R
import khaled.benzo.data.model.DownloadItem
import khaled.benzo.data.model.DownloadStatus
import khaled.benzo.databinding.ItemDownloadBinding
import java.util.Locale

class DownloadAdapter(
    private val onItemClick: (DownloadItem) -> Unit,
    private val onControlClick: (DownloadItem) -> Unit,
    private val onMenuClick: (DownloadItem, View) -> Unit
) : ListAdapter<DownloadItem, DownloadAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDownloadBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemDownloadBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DownloadItem) {
            binding.txtFileName.text = item.fileName
            binding.txtStatusSubtitle.text = "${item.status.name} · ${item.parts} parts · ${item.url}"

            // Progress calculation
            val percent = if (item.totalBytes > 0) {
                ((item.downloadedBytes.toDouble() / item.totalBytes.toDouble()) * 100).toInt()
            } else 0

            binding.progressBar.progress = percent
            binding.txtSizes.text = String.format(
                Locale.US,
                "%s / %s (%d%%)",
                formatBytes(item.downloadedBytes),
                if (item.totalBytes > 0) formatBytes(item.totalBytes) else "?",
                percent
            )

            binding.txtSpeed.text = if (item.status == DownloadStatus.RUNNING) {
                String.format(Locale.US, "%s/s", formatBytes(item.speed))
            } else ""

            binding.txtEta.text = if (item.status == DownloadStatus.RUNNING && item.speed > 0 && item.totalBytes > 0) {
                val remainingBytes = item.totalBytes - item.downloadedBytes
                val seconds = remainingBytes / item.speed
                String.format(Locale.US, "ETA: %02d:%02d", seconds / 60, seconds % 60)
            } else ""

            // Status icon for control button
            val controlRes = when (item.status) {
                DownloadStatus.RUNNING -> R.drawable.stat_pause
                DownloadStatus.COMPLETED -> R.drawable.stat_success
                else -> R.drawable.stat_start
            }
            binding.btnControl.setImageResource(controlRes)

            binding.root.setOnClickListener { onItemClick(item) }
            binding.btnControl.setOnClickListener { onControlClick(item) }
            binding.btnItemMenu.setOnClickListener { v -> onMenuClick(item, v) }
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

    companion object DiffCallback : DiffUtil.ItemCallback<DownloadItem>() {
        override fun areItemsTheSame(oldItem: DownloadItem, newItem: DownloadItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: DownloadItem, newItem: DownloadItem): Boolean =
            oldItem == newItem
    }
}
