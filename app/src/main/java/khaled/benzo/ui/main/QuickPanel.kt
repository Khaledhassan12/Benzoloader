package khaled.benzo.ui.main

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.material.color.MaterialColors
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import khaled.benzo.R
import khaled.benzo.core.download.SpeedTracker
import khaled.benzo.data.model.DownloadItem
import khaled.benzo.data.model.DownloadStatus
import khaled.benzo.data.repository.SettingsRepository

/** Line graph of the total download speed over the last samples. Axis label = the highest value shown. WRITTEN, UNBUILT, never rendered. */
class SpeedGraphView(context: Context) : View(context) {
    private val samples = ArrayDeque<Long>()
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 3f * resources.displayMetrics.density
        color = MaterialColors.getColor(context, androidx.appcompat.R.attr.colorPrimary, 0xFF4F8EF7.toInt())
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11f * resources.displayMetrics.scaledDensity
        color = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF888888.toInt())
    }

    fun add(bytesPerSecond: Long) { samples.addLast(bytesPerSecond.coerceAtLeast(0)); while (samples.size > MAX) samples.removeFirst(); invalidate() }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val top = samples.maxOrNull()?.takeIf { it > 0 } ?: 1L
        c.drawText(SpeedTracker.formatSize(top) + "/s", 4f, text.textSize, text)
        if (samples.size < 2) return
        val p = Path()
        samples.forEachIndexed { i, v ->
            val x = w * i / (MAX - 1).toFloat()
            val y = h - (h - text.textSize * 1.5f) * v / top - 2f
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        c.drawPath(p, line)
    }

    companion object { const val MAX = 60 }
}

/**
 * The quick-settings panel (the left menu in the reference videos): a speed graph and an overall bar on top, collapsible
 * sections of switches and sliders that read and write the SAME stored settings as the Settings screens, and a bottom bar
 * with Exit, Settings and Browser. Layout and order follow video 1-3 notes in docs/UI_PARITY_PLAN.md; words are our own.
 * NOT built here (gaps): the per-row visibility editor (eye icon), Smart download, Download torrents, pop-up messages and
 * download icons switches, Material You and Dark theme rows, Batch loading, the App section. WRITTEN, UNBUILT, never rendered.
 */
class QuickPanel(
    context: Context,
    private val settings: SettingsRepository,
    private val onOpenSettings: () -> Unit,
    private val onOpenBrowser: () -> Unit,
    private val onOpenSiteManager: () -> Unit,
    private val onOpenBatch: () -> Unit,
    private val onOpenTopWindow: () -> Unit,
    private val onExit: () -> Unit,
    private val onChanged: () -> Unit
) : LinearLayout(context) {

    private enum class Section { DOWNLOADING, TORRENT, NOTIFICATIONS, OPTIONS, INTERFACE, TOOLS }

    private val open = mutableSetOf(Section.DOWNLOADING)
    private val graph = SpeedGraphView(context)
    private val barText = TextView(context)
    private val bar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100 }
    private val body = LinearLayout(context).apply { orientation = VERTICAL }

    init {
        orientation = VERTICAL
        addView(graph, LayoutParams(-1, dp(96)).apply { setMargins(dp(16), dp(16), dp(16), 0) })
        barText.setPadding(dp(16), dp(6), dp(16), 0)
        addView(barText); addView(bar, LayoutParams(-1, dp(8)).apply { setMargins(dp(16), dp(4), dp(16), dp(8)) })
        val scroll = android.widget.ScrollView(context).apply { addView(body) }
        addView(scroll, LayoutParams(-1, 0, 1f))
        addView(bottomBar())
        renderSections()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    /** Called whenever the list changes: one more graph sample and a new overall bar from the running downloads only. */
    fun update(items: List<DownloadItem>) {
        val running = items.filter { it.status == DownloadStatus.RUNNING }
        graph.add(running.sumOf { it.speed })
        val done = running.sumOf { it.downloadedBytes }
        val total = running.sumOf { if (it.totalBytes > 0) it.totalBytes else 0L }
        val pct = if (total > 0) (done * 100 / total).toInt().coerceIn(0, 100) else 0
        bar.progress = pct
        barText.text = if (total > 0) "${SpeedTracker.formatSize(done)} | $pct% | ${SpeedTracker.formatSize(total)}" else context.getString(R.string.qp_idle)
    }

    /** Re-reads every stored value; called when the panel is opened so a change made in Settings shows up. */
    fun refresh() = renderSections()

    private fun renderSections() {
        graph.visibility = if (settings.quickGraphVisible) VISIBLE else GONE
        body.removeAllViews()
        section(Section.DOWNLOADING, R.string.qp_downloading) {
            valueRow(R.string.qp_folder, settings.defaultDownloadPath.ifEmpty { context.getString(R.string.qp_default_folder) }) { onOpenSettings() }
            sliderRow(R.string.qp_simultaneous, 1, 5, settings.maxConcurrentDownloads, { "$it" }) { settings.maxConcurrentDownloads = it }
            sliderRow(R.string.qp_threads, 1, 16, settings.defaultThreadsCount, { "$it" }) { settings.defaultThreadsCount = it }
            sliderRow(R.string.qp_speed, 0, 10240, settings.speedLimitKBps, { if (it == 0) context.getString(R.string.qp_speed_max) else "$it KB/s" }, 64) { settings.speedLimitKBps = it }
            switchRow(R.string.qp_auto_resume, settings.autoResume) { settings.autoResume = it }
            val labels = arrayOf(context.getString(R.string.after_finish_nothing), context.getString(R.string.after_finish_exit))
            valueRow(R.string.row_after_finish, labels[settings.afterFinish.coerceIn(0, 1)]) {
                com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.row_after_finish)
                    .setSingleChoiceItems(labels, settings.afterFinish.coerceIn(0, 1)) { dlg, which -> settings.afterFinish = which; dlg.dismiss(); renderSections(); onChanged() }
                    .setNegativeButton(R.string.dialog_cancel, null)
                    .show()
            }
        }
        section(Section.TORRENT, R.string.qp_torrent) {
            switchRow(R.string.qp_sequential, settings.torrentSequential) { settings.torrentSequential = it }
            switchRow(R.string.qp_seeding, settings.torrentSeedAfterFinish) { settings.torrentSeedAfterFinish = it }
        }
        section(Section.NOTIFICATIONS, R.string.qp_notifications) {
            switchRow(R.string.qp_vibration, settings.vibrateOnComplete) { settings.vibrateOnComplete = it }
            switchRow(R.string.qp_sounds, settings.soundOnComplete) { settings.soundOnComplete = it }
        }
        section(Section.OPTIONS, R.string.qp_options) {
            switchRow(R.string.qp_wifi_only, settings.isWifiOnly) { settings.isWifiOnly = it }
            switchRow(R.string.qp_cataloging, settings.catalogEnabled) { settings.catalogEnabled = it }
            switchRow(R.string.qp_scheduler, settings.scheduleEnabled) { settings.scheduleEnabled = it }
            // Turning the overlay on may need the draw-over-apps permission: the Top window screen asks for it, so enabling here opens it.
            switchRow(R.string.qp_top_window, settings.topWindowEnabled) { on ->
                settings.topWindowEnabled = on
                if (on && !android.provider.Settings.canDrawOverlays(context)) onOpenTopWindow()
            }
        }
        section(Section.INTERFACE, R.string.qp_interface) {
            // Theme row (light, dark or system) writes the same setting as Settings > Interface and applies at once.
            val modes = arrayOf(context.getString(R.string.mode_system), context.getString(R.string.mode_light), context.getString(R.string.mode_dark))
            valueRow(R.string.qp_theme, modes[settings.themeMode.coerceIn(0, 2)]) {
                com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.qp_theme)
                    .setSingleChoiceItems(modes, settings.themeMode.coerceIn(0, 2)) { dlg, which ->
                        settings.themeMode = which; dlg.dismiss()
                        khaled.benzo.ui.common.ThemeColors.applyNightMode(context)
                    }
                    .setNegativeButton(R.string.dialog_cancel, null).show()
            }
            switchRow(R.string.qp_bottom_bar, settings.bottomBar) { settings.bottomBar = it }
            switchRow(R.string.qp_swap_menus, settings.swapSideMenus) { settings.swapSideMenus = it }
        }
        section(Section.TOOLS, R.string.qp_tools) {
            valueRow(R.string.qp_site_manager, null) { onOpenSiteManager() }
            valueRow(R.string.qp_batch, null) { onOpenBatch() }
        }
    }

    private fun section(which: Section, title: Int, rows: () -> Unit) {
        val header = TextView(context).apply {
            setText(title); textSize = 16f; gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(52); setPadding(dp(16), 0, dp(16), 0)
            text = (if (which in open) "\u25BE  " else "\u25B8  ") + context.getString(title)
            isClickable = true
            setOnClickListener { if (!open.add(which)) open.remove(which); renderSections() }
        }
        if (which == Section.DOWNLOADING) {
            // OBSERVED (video 3): a chart icon in this header shows or hides the graph. The icon here is a stock placeholder, not a match.
            val row = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
            row.addView(header, LayoutParams(0, -2, 1f))
            row.addView(android.widget.ImageButton(context).apply {
                setImageResource(android.R.drawable.ic_menu_sort_by_size)
                setBackgroundResource(android.R.color.transparent)
                contentDescription = context.getString(R.string.qp_graph_toggle)
                setOnClickListener { settings.quickGraphVisible = !settings.quickGraphVisible; renderSections() }
            }, LayoutParams(dp(48), dp(48)))
            body.addView(row)
        } else body.addView(header)
        if (which in open) rows()
    }

    private fun valueRow(title: Int, value: String?, onTap: () -> Unit) {
        val tv = TextView(context).apply {
            text = context.getString(title) + (value?.let { "\n$it" } ?: "")
            minimumHeight = dp(52); gravity = Gravity.CENTER_VERTICAL; setPadding(dp(32), dp(6), dp(16), dp(6))
            isClickable = true; setOnClickListener { onTap() }
        }
        body.addView(tv)
    }

    private fun switchRow(title: Int, value: Boolean, set: (Boolean) -> Unit) {
        val sw = MaterialSwitch(context).apply {
            setText(title); isChecked = value; minimumHeight = dp(52); setPadding(dp(32), 0, dp(16), 0)
            setOnCheckedChangeListener { _, on -> set(on); onChanged() }
        }
        body.addView(sw)
    }

    private fun sliderRow(title: Int, min: Int, max: Int, value: Int, fmt: (Int) -> String, step: Int = 1, set: (Int) -> Unit) {
        val label = TextView(context).apply { setPadding(dp(32), dp(6), dp(16), 0) }
        val v = value.coerceIn(min, max)
        label.text = context.getString(title) + ": " + fmt(v)
        val s = Slider(context).apply {
            valueFrom = min.toFloat(); valueTo = max.toFloat(); stepSize = step.toFloat(); this.value = v.toFloat()
            addOnChangeListener { _, x, _ -> label.text = context.getString(title) + ": " + fmt(x.toInt()) }
            addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {}
                override fun onStopTrackingTouch(slider: Slider) { set(slider.value.toInt()); onChanged() }
            })
        }
        body.addView(label)
        body.addView(s, LayoutParams(-1, -2).apply { setMargins(dp(24), 0, dp(8), 0) })
    }

    private fun bottomBar(): View = LinearLayout(context).apply {
        orientation = HORIZONTAL; gravity = Gravity.CENTER
        fun btn(t: Int, a: () -> Unit) = addView(TextView(context).apply {
            setText(t); gravity = Gravity.CENTER; minimumHeight = dp(56); isClickable = true; setOnClickListener { a() }
        }, LayoutParams(0, -2, 1f))
        btn(R.string.qp_exit) { onExit() }
        btn(R.string.qp_settings) { onOpenSettings() }
        btn(R.string.qp_browser) { onOpenBrowser() }
    }
}
