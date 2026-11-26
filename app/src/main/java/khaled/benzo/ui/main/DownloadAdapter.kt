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

        private val firstSeen = HashMap<Long, Triple<Long, Long, Int>>()
        private val stockName = binding.txtFileName.textColors
        private val stockInfo = binding.txtStatusSubtitle.textColors

        /** Custom colours from Interface colours; 0 = stock. Applied on every bind so a change shows when the list redraws. */
        private fun applyColors() {
            val s = khaled.benzo.data.repository.SettingsRepository(binding.root.context)
            if (s.uiColFilename != 0) binding.txtFileName.setTextColor(s.uiColFilename) else binding.txtFileName.setTextColor(stockName)
            for (t in listOf(binding.txtStatusSubtitle, binding.txtSizes, binding.txtSpeed, binding.txtEta))
                if (s.uiColInfo != 0) t.setTextColor(s.uiColInfo) else t.setTextColor(stockInfo)
        }

        fun bind(item: DownloadItem) {
            applyColors()
            binding.txtFileName.text = item.fileName
            val stats = khaled.benzo.data.repository.SettingsRepository(binding.root.context)
            val added = if (stats.statAddedTime && item.createdDate > 0)
                " \u00B7 " + android.text.format.DateFormat.format("MM-dd HH:mm", item.createdDate) else ""
            binding.txtStatusSubtitle.text = "${khaled.benzo.ui.common.StatusText.of(binding.root.context, item.status, waiting[item.id])}" + added
            // Reference frame: a finished row has no second text line (the check icon says it). Other states keep their status text.
            binding.txtStatusSubtitle.visibility = if (item.status == DownloadStatus.COMPLETED && added.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE

            // Progress calculation
            val percent = if (item.totalBytes > 0) {
                ((item.downloadedBytes.toDouble() / item.totalBytes.toDouble()) * 100).toInt()
            } else 0

            binding.progressBar.progress = if (item.status == DownloadStatus.COMPLETED) 100 else percent
            binding.progressBar.progressDrawable = androidx.core.content.ContextCompat.getDrawable(
                binding.root.context,
                when (item.status) {
                    DownloadStatus.COMPLETED -> R.drawable.bar_done
                    DownloadStatus.PAUSED, DownloadStatus.ERROR -> R.drawable.bar_failed
                    else -> R.drawable.bar_running
                }
            )
            binding.progressBar.isIndeterminate = false
            // Custom bar colours apply to the running bar only; the finished bar keeps its own look (the reference showed no rule).
            khaled.benzo.data.repository.SettingsRepository(binding.root.context).let { s ->
                if (item.status != DownloadStatus.COMPLETED) {
                    if (s.uiColProgress != 0) binding.progressBar.progressTintList = android.content.res.ColorStateList.valueOf(s.uiColProgress)
                    if (s.uiColProgressBg != 0) binding.progressBar.progressBackgroundTintList = android.content.res.ColorStateList.valueOf(s.uiColProgressBg)
                }
            }
            val total = if (item.totalBytes > 0) formatBytes(item.totalBytes) else "?"
            // Finished: "size · average speed · time" (reference frame). The speed and time exist only for downloads recorded since the transfer time was stored.
            val secs = item.activeMs / 1000
            binding.txtSizes.text = if (item.status == DownloadStatus.COMPLETED) {
                if (item.activeMs >= 1000 && item.totalBytes > 0)
                    total + " \u00B7 " + khaled.benzo.core.download.SpeedFormat.text(item.totalBytes / secs, null, stats.speedShowMask) + " \u00B7 " + clock(secs)
                else total
            } else formatBytes(item.downloadedBytes) + " / " + total
            val running = item.status == DownloadStatus.RUNNING
            binding.txtSpeed.text = if (running) {
                // Average since this row was first seen running in this session (INFERRED; no stored start time exists).
                val now = System.currentTimeMillis()
                val base = firstSeen.getOrPut(item.id) { Triple(now, item.downloadedBytes, 0) }
                val secs = (now - base.first) / 1000
                val avg = if (secs >= 3) ((item.downloadedBytes - base.second).coerceAtLeast(0L) / secs) else null
                khaled.benzo.core.download.SpeedFormat.text(item.speed, avg, stats.speedShowMask)
            } else { firstSeen.remove(item.id); "" }
            // remaining time from aggregate bytes and current speed only
            binding.txtEta.text = if (running && item.speed > 0 && item.totalBytes > 0) {
                val seconds = (item.totalBytes - item.downloadedBytes).coerceAtLeast(0) / item.speed
                String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60)
            } else if (item.status == DownloadStatus.PAUSED || item.status == DownloadStatus.ERROR) {
                // Reference frame (stopped row): "elapsed / remaining".
                clock(secs) + " / " + (if (item.speed > 0 && item.totalBytes > 0) clock((item.totalBytes - item.downloadedBytes).coerceAtLeast(0) / item.speed) else "00:00")
            } else if (stats.statFinishTime && item.status == DownloadStatus.COMPLETED && item.completedDate > 0) {
                java.text.SimpleDateFormat("MMM d, yyyy h:mm:ss a", Locale.getDefault()).format(java.util.Date(item.completedDate))
            } else ""

            binding.imgFileIcon.setImageResource(when (item.status) {
                DownloadStatus.RUNNING -> R.drawable.stat_pause
                DownloadStatus.COMPLETED -> R.drawable.stat_success
                DownloadStatus.PAUSED -> R.drawable.stat_stop
                DownloadStatus.ERROR -> R.drawable.stat_error
                else -> R.drawable.stat_start
            })
            binding.imgFileIcon.setOnClickListener { onControlClick(item) }

            binding.root.setOnClickListener { onItemClick(item) }
            // Long press opens the item menu (the overflow button moved here); "Select" in that menu starts selection mode.
            binding.root.setOnLongClickListener { onMenuClick(item, binding.root); true }
            if (item.id in selected) {
                binding.root.setBackgroundColor(com.google.android.material.color.MaterialColors.getColor(
                    binding.root, com.google.android.material.R.attr.colorSecondaryContainer))
            } else {
                val tv = android.util.TypedValue()
                binding.root.context.theme.resolveAttribute(android.R.attr.selectableItemBackground, tv, true)
                binding.root.setBackgroundResource(tv.resourceId)
            }
            binding.imgFileType.setImageResource(fileTypeIcon(item))
        }

        private fun clock(totalSeconds: Long): String =
            if (totalSeconds >= 3600) String.format(Locale.US, "%d:%02d:%02d", totalSeconds / 3600, totalSeconds % 3600 / 60, totalSeconds % 60)
            else String.format(Locale.US, "%02d:%02d", totalSeconds / 60, totalSeconds % 60)

        /** File-type icon on the right of the row (reference frames: blue phone = programs, pink document = text; colours from the reference). */
        private fun fileTypeIcon(item: DownloadItem): Int {
            if (item.isTorrent) return R.drawable.ic_filetype_torr
            return when (khaled.benzo.data.model.FileCategory.fromExtension(item.fileName)) {
                khaled.benzo.data.model.FileCategory.ARCHIVE -> R.drawable.ic_filetype_archive
                khaled.benzo.data.model.FileCategory.PROGRAM -> R.drawable.ic_filetype_program
                khaled.benzo.data.model.FileCategory.VIDEO -> R.drawable.ic_filetype_video
                khaled.benzo.data.model.FileCategory.MUSIC -> R.drawable.ic_filetype_music
                khaled.benzo.data.model.FileCategory.IMAGE -> R.drawable.ic_filetype_image
                khaled.benzo.data.model.FileCategory.DOCUMENT -> R.drawable.ic_filetype_text
                khaled.benzo.data.model.FileCategory.TORRENT -> R.drawable.ic_filetype_torr
                else -> R.drawable.ic_filetype_other
            }
        }

        private fun formatBytes(bytes: Long): String = khaled.benzo.core.download.SizeFormat.format(bytes, 1)
    }

    companion object DiffCallback : DiffUtil.ItemCallback<DownloadItem>() {
        override fun areItemsTheSame(oldItem: DownloadItem, newItem: DownloadItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: DownloadItem, newItem: DownloadItem): Boolean =
            oldItem == newItem
    }
}
