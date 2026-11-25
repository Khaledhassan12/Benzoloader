package khaled.benzo.ui.main

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.color.MaterialColors
import khaled.benzo.R
import khaled.benzo.core.queue.ListLogic
import khaled.benzo.core.queue.ListState
import khaled.benzo.core.queue.SortKey
import khaled.benzo.core.queue.StatusFilter
import khaled.benzo.core.queue.TimeFilter
import khaled.benzo.data.model.DownloadItem
import khaled.benzo.data.model.FileCategory

/**
 * The side panel with four collapsible sections: Category, Status, Time and Sorting. One section is open at a time. A row
 * shows its count only when the count is not zero. The selected row is a rounded pill in the accent colour.
 * Layout, order and behaviour follow the reference captures (docs/UI_PARITY_PLAN.md); the words are our own and rows have
 * no icons yet. Unrendered.
 */
class FilterPanel(
    context: Context,
    private val onType: (FileCategory?) -> Unit,
    private val onStatus: (StatusFilter) -> Unit,
    private val onTime: (TimeFilter) -> Unit,
    private val onSort: (SortKey, Boolean) -> Unit,
    private val onClear: () -> Unit
) : LinearLayout(context) {

    private enum class Section { CATEGORY, STATUS, TIME, SORTING }

    private var open: Section? = Section.STATUS
    private var state = ListState()
    private var items: List<DownloadItem> = emptyList()
    private var now = System.currentTimeMillis()

    init { orientation = VERTICAL }

    fun render(state: ListState, items: List<DownloadItem>, nowMillis: Long = System.currentTimeMillis()) {
        this.state = state; this.items = items; this.now = nowMillis
        removeAllViews()
        section(Section.CATEGORY, R.string.panel_category) { categoryRows() }
        section(Section.STATUS, R.string.panel_status) { statusRows() }
        section(Section.TIME, R.string.panel_time) { timeRows() }
        section(Section.SORTING, R.string.panel_sorting) { sortRows() }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun section(which: Section, title: Int, rows: () -> Unit) {
        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(64)
            setPadding(dp(24), 0, dp(24), 0)
            setOnClickListener { open = if (open == which) null else which; render(state, items, now) }
        }
        header.addView(TextView(context).apply {
            setText(title); textSize = 16f
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        })
        if (which == Section.CATEGORY && filterActive()) {
            header.addView(TextView(context).apply {
                setText(R.string.panel_clear); setPadding(dp(12), dp(8), dp(12), dp(8))
                setTextColor(accent()); setOnClickListener { onClear() }
            })
        }
        header.addView(TextView(context).apply { text = if (open == which) "\u25B4" else "\u25BE"; textSize = 18f })
        addView(header)
        if (open == which) rows()
        addView(View(context).apply {
            setBackgroundColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutlineVariant))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(1)).apply { setMargins(dp(24), 0, dp(24), 0) }
        })
    }

    private fun filterActive() = state.status != StatusFilter.ALL || state.type != null || state.time != TimeFilter.ALL

    private fun accent() = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary)

    private fun row(label: String, count: Int?, selected: Boolean, onClick: () -> Unit) {
        val r = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(56)
            setPadding(dp(24), 0, dp(24), 0)
            if (selected) background = GradientDrawable().apply {
                cornerRadius = dp(28).toFloat()
                setColor(MaterialColors.getColor(this@FilterPanel, com.google.android.material.R.attr.colorSurfaceVariant))
            }
            setOnClickListener { onClick() }
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { setMargins(dp(8), dp(2), dp(8), dp(2)) }
        }
        val color = if (selected) accent() else MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface)
        r.addView(TextView(context).apply {
            text = label; textSize = 15f; setTextColor(color)
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        })
        if (count != null && count > 0) r.addView(TextView(context).apply { text = count.toString(); setTextColor(color) })
        addView(r)
    }

    private fun categoryRows() {
        val list = listOf(
            null to R.string.nav_all, FileCategory.OTHER to R.string.nav_other, FileCategory.ARCHIVE to R.string.nav_archives,
            FileCategory.PROGRAM to R.string.nav_programs, FileCategory.VIDEO to R.string.nav_video, FileCategory.MUSIC to R.string.nav_music,
            FileCategory.IMAGE to R.string.nav_images, FileCategory.DOCUMENT to R.string.nav_documents, FileCategory.TORRENT to R.string.nav_torrent
        )
        for ((cat, label) in list) row(context.getString(label), null, state.type == cat) { onType(cat) }
    }

    /** Stopped = paused, Running = connecting or running, Failed = error, Successful = completed. The Stopped mapping is inferred. */
    private fun statusRows() {
        val list = listOf(
            StatusFilter.ALL to R.string.nav_all, StatusFilter.PAUSED to R.string.panel_stopped, StatusFilter.ACTIVE to R.string.panel_running,
            StatusFilter.FAILED to R.string.nav_failed, StatusFilter.COMPLETED to R.string.panel_successful
        )
        for ((f, label) in list) {
            val count = items.count { ListLogic.matches(it, f) }
            row(context.getString(label), count, state.status == f) { onStatus(f) }
        }
    }

    private fun timeRows() {
        val counts = TimeFilter.counts(items, now)
        val names = mapOf(
            TimeFilter.ALL to R.string.nav_all, TimeFilter.TODAY to R.string.panel_today, TimeFilter.YESTERDAY to R.string.panel_yesterday,
            TimeFilter.WEEK to R.string.panel_week, TimeFilter.MONTH to R.string.panel_month, TimeFilter.YEAR to R.string.panel_year
        )
        for (f in TimeFilter.values()) row(context.getString(names.getValue(f)), counts[f], state.time == f) { onTime(f) }
    }

    /** Ten entries, each key in a down and an up variant, as in the reference. Down = newest, largest or Z first, up = the reverse. */
    private fun sortRows() {
        val keys = listOf(SortKey.ORDER to R.string.panel_sort_order, SortKey.SIZE to R.string.panel_sort_size, SortKey.NAME to R.string.panel_sort_name,
            SortKey.TIME to R.string.panel_sort_time, SortKey.CATEGORY to R.string.panel_sort_category)
        for ((key, label) in keys) for (ascending in listOf(false, true)) {
            val arrow = if (ascending) "\u2191 " else "\u2193 "
            row(arrow + context.getString(label), null, state.sort == key && state.ascending == ascending) { onSort(key, ascending) }
        }
    }
}
