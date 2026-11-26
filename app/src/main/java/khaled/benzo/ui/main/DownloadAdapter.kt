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
    private val onMenuClick: (DownloadItem, View) -> Unit,
    private val onItemLongClick: (DownloadItem) -> Unit = {}
) : ListAdapter<DownloadItem, DownloadAdapter.ViewHolder>(DiffCallback) {

    private var selected: Set<Long> = emptySet()

    /** Selected rows get a tinted background. Only the rows are redrawn, the list itself is unchanged. */
    @android.annotation.SuppressLint("NotifyDataSetChanged")
    fun setSelected(ids: Set<Long>) { if (ids != selected) { selected = ids; notifyDataSetChanged() } }

    private var waiting: Map<Long, khaled.benzo.core.queue.BlockReason> = emptyMap()

    /** New reasons for queued items: only the rows that are visible need a redraw. */
    @android.annotation.SuppressLint("NotifyDataSetChanged")
    fun setWaiting(map: Map<Long, khaled.benzo.core.queue.BlockReason>) { if (map != waiting) { waiting = map; notifyDataSetChanged() } }

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
            binding.txtStatusSubtitle.text = "${khaled.benzo.ui.common.StatusText.of(binding.root.context, item.status, waiting[item.id])}"

            // Progress calculation
            val percent = if (item.totalBytes > 0) {
                ((item.downloadedBytes.toDouble() / item.totalBytes.toDouble()) * 100).toInt()
            } else 0

            binding.progressBar.progress = if (item.status == DownloadStatus.COMPLETED) 100 else percent
            binding.progressBar.progressDrawable = androidx.core.content.ContextCompat.getDrawable(
                binding.root.context,
                if (item.status == DownloadStatus.COMPLETED) R.drawable.bar_done else R.drawable.bar_running
            )
            binding.progressBar.isIndeterminate = false
            val total = if (item.totalBytes > 0) formatBytes(item.totalBytes) else "?"
            binding.txtSizes.text = if (item.status == DownloadStatus.COMPLETED) total
                else formatBytes(item.downloadedBytes) + " / " + total
            val running = item.status == DownloadStatus.RUNNING
            binding.txtSpeed.text = if (running) String.format(Locale.US, "%s/s", formatBytes(item.speed)) else ""
            // remaining time from aggregate bytes and current speed only
            binding.txtEta.text = if (running && item.speed > 0 && item.totalBytes > 0) {
                val seconds = (item.totalBytes - item.downloadedBytes).coerceAtLeast(0) / item.speed
                String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60)
            } else if (item.status == DownloadStatus.COMPLETED && item.completedDate > 0) {
                android.text.format.DateFormat.format("yyyy-MM-dd HH:mm", item.completedDate).toString()
            } else ""

            binding.imgFileIcon.setImageResource(when (item.status) {
                DownloadStatus.RUNNING -> R.drawable.stat_pause
                DownloadStatus.COMPLETED -> R.drawable.stat_success
                else -> R.drawable.stat_start
            })
            binding.imgFileIcon.setOnClickListener { onControlClick(item) }

            binding.root.setOnClickListener { onItemClick(item) }
            binding.root.setOnLongClickListener { onItemLongClick(item); true }
            if (item.id in selected) {
                binding.root.setBackgroundColor(com.google.android.material.color.MaterialColors.getColor(
                    binding.root, com.google.android.material.R.attr.colorSecondaryContainer))
            } else {
                val tv = android.util.TypedValue()
                binding.root.context.theme.resolveAttribute(android.R.attr.selectableItemBackground, tv, true)
                binding.root.setBackgroundResource(tv.resourceId)
            }
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
