package khaled.benzo.ui.settings

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import khaled.benzo.R
import khaled.benzo.core.overlay.TopStat
import khaled.benzo.core.overlay.TopWindowSpec
import khaled.benzo.data.repository.SettingsRepository

/**
 * Top window settings: a transparent progress strip drawn over other apps while a download runs.
 * Row order follows the reference video (switch, statistics, three colours, opacity, width, line height, text size,
 * alignment, offsets). Built in code so no layout or binding changes are needed. WRITTEN, UNBUILT, never rendered.
 * Interim difference: colours are typed as hex; the reference has a colour square picker (not built).
 */
class TopWindowSettingsActivity : AppCompatActivity() {
    private lateinit var repo: SettingsRepository
    private lateinit var list: LinearLayout
    private val refreshers = ArrayList<() -> Unit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = SettingsRepository(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val toolbar = com.google.android.material.appbar.MaterialToolbar(this).apply {
            setTitle(R.string.cat_top_window)
            setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
            setNavigationOnClickListener { finish() }
        }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(56)))
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(list) }, LinearLayout.LayoutParams(-1, -1))
        setContentView(root)
        build()
    }

    override fun onResume() {
        super.onResume()
        refreshers.forEach { it() }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    /** Title with the current value underneath, tap to edit. */
    private fun valueRow(title: Int, value: () -> String, onTap: () -> Unit, enabledIf: () -> Boolean = { true }): TextView {
        val tv = TextView(this).apply {
            minHeight = dp(72); gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(12)); isClickable = true
            val t = TypedValue(); theme.resolveAttribute(android.R.attr.selectableItemBackground, t, true); setBackgroundResource(t.resourceId)
            setOnClickListener { onTap() }
        }
        val refresh = {
            tv.text = getString(title) + "\n" + value()
            tv.alpha = if (enabledIf()) 1f else 0.38f; tv.isEnabled = enabledIf()
        }
        refreshers += refresh; refresh()
        list.addView(tv)
        return tv
    }

    private fun build() {
        // 1. Show over other apps (switch). Turning it on asks for the overlay permission when missing.
        val sw = MaterialSwitch(this).apply {
            text = getString(R.string.top_show); isChecked = repo.topWindowEnabled
            setPadding(dp(20), dp(12), dp(20), dp(12)); minHeight = dp(72)
        }
        sw.setOnCheckedChangeListener { _, on ->
            repo.topWindowEnabled = on
            if (on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) askPermission()
            refreshers.forEach { it() }
        }
        list.addView(sw)
        val on = { repo.topWindowEnabled }

        valueRow(R.string.top_stats, { statsSummary() }, { pickStats() }, on)
        colorRow(R.string.top_text_color, { repo.topWindowTextColor }, { repo.topWindowTextColor = it }, on, 0xFFFFFFFF.toInt())
        colorRow(R.string.top_bg_color, { repo.topWindowBackgroundColor }, { repo.topWindowBackgroundColor = it }, on, 0xFF000000.toInt())
        colorRow(R.string.top_bar_color, { repo.topWindowBarColor }, { repo.topWindowBarColor = it }, on, 0xFFE53935.toInt())
        sliderRow(R.string.top_opacity, 10, 100, { repo.topWindowOpacityPercent }, { repo.topWindowOpacityPercent = it }, { "$it%" }, on)
        val sw2 = resources.displayMetrics.widthPixels
        val sh = resources.displayMetrics.heightPixels
        sliderRow(R.string.top_width, 0, sw2, { repo.topWindowWidthPx }, { repo.topWindowWidthPx = it },
            { if (it == 0) getString(R.string.top_full_width) else "$it px" }, on)
        sliderRow(R.string.top_line_height, 1, 128, { repo.topWindowLineHeightPx }, { repo.topWindowLineHeightPx = it }, { "$it px" }, on)
        sliderRow(R.string.top_text_size, 1, 32, { repo.topWindowTextSizePx }, { repo.topWindowTextSizePx = it }, { "$it px" }, on)
        choiceRow(R.string.top_h_pos, listOf(R.string.pos_start, R.string.pos_center, R.string.pos_end),
            { repo.topWindowHAlign }, { repo.topWindowHAlign = it }, on)
        choiceRow(R.string.top_v_pos, listOf(R.string.pos_top, R.string.pos_center, R.string.pos_bottom),
            { repo.topWindowVAlign }, { repo.topWindowVAlign = it }, on)
        sliderRow(R.string.top_offset_x, 0, sw2, { repo.topWindowOffsetX }, { repo.topWindowOffsetX = it },
            { if (it == 0) getString(R.string.top_off) else "$it px" }, on)
        sliderRow(R.string.top_offset_y, 0, sh, { repo.topWindowOffsetY }, { repo.topWindowOffsetY = it },
            { if (it == 0) getString(R.string.top_off) else "$it px" }, on)
    }

    private fun askPermission() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.top_perm_title).setMessage(R.string.top_perm_msg)
            .setPositiveButton(R.string.top_perm_open) { _, _ ->
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }
            .setNegativeButton(android.R.string.cancel, null).show()
    }

    private fun statsSummary(): String {
        val names = listOf(R.string.stat_percent, R.string.stat_downloaded, R.string.stat_size, R.string.stat_speed, R.string.stat_elapsed, R.string.stat_left)
        val chosen = TopStat.values().filter { repo.topWindowStatsMask and (1 shl it.ordinal) != 0 }.map { getString(names[it.ordinal]) }
        return if (chosen.isEmpty()) getString(R.string.top_off) else chosen.joinToString(" \u00B7 ")
    }

    private fun pickStats() {
        val names = listOf(R.string.stat_percent, R.string.stat_downloaded, R.string.stat_size, R.string.stat_speed, R.string.stat_elapsed, R.string.stat_left)
            .map { getString(it) }.toTypedArray()
        val checked = BooleanArray(names.size) { repo.topWindowStatsMask and (1 shl it) != 0 }
        MaterialAlertDialogBuilder(this).setTitle(R.string.top_stats)
            .setMultiChoiceItems(names, checked) { _, i, c -> checked[i] = c }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                repo.topWindowStatsMask = checked.indices.fold(0) { m, i -> if (checked[i]) m or (1 shl i) else m }
                refreshers.forEach { it() }
            }.setNegativeButton(android.R.string.cancel, null).show()
    }

    private fun colorRow(title: Int, get: () -> Int, set: (Int) -> Unit, enabled: () -> Boolean, default: Int) {
        val row = valueRow(title, { "#%08X".format(get()) }, {
            // Shared picker; its Presets list carries "Default", which replaces the old Reset button.
            khaled.benzo.ui.common.ColorPickerDialog.show(this, title, get(), default) { picked -> set(picked); refreshers.forEach { it() } }
        }, enabled)
        // OBSERVED (video 5): trailing colour square, and a cross left of it only while the value differs from default.
        // Layout: [row text][cross][square]. The cross resets to the default; the square opens the picker like the row.
        val index = list.indexOfChild(row)
        list.removeView(row)
        val square = View(this)
        val cross = android.widget.ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setBackgroundResource(android.R.color.transparent)
            contentDescription = getString(R.string.top_color_reset)
            setOnClickListener { set(default); refreshers.forEach { it() } }
        }
        val wrap = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        wrap.addView(row, LinearLayout.LayoutParams(0, -2, 1f))
        wrap.addView(cross, LinearLayout.LayoutParams(dp(40), dp(40)))
        wrap.addView(square, LinearLayout.LayoutParams(dp(28), dp(28)).apply { marginStart = dp(4); marginEnd = dp(20) })
        square.setOnClickListener { row.performClick() }
        list.addView(wrap, index)
        val paintTrailing = {
            square.background = android.graphics.drawable.GradientDrawable().apply { setColor(get()); setStroke(dp(1), 0x66808080) }
            cross.visibility = if (get() != default) View.VISIBLE else View.INVISIBLE
            val a = if (enabled()) 1f else 0.38f; cross.alpha = a; square.alpha = a; cross.isEnabled = enabled(); square.isEnabled = enabled()
        }
        paintTrailing(); refreshers += paintTrailing
    }

    private fun sliderRow(title: Int, min: Int, max: Int, get: () -> Int, set: (Int) -> Unit, fmt: (Int) -> String, enabled: () -> Boolean) {
        valueRow(title, { fmt(get().coerceIn(min, max)) }, {
            val label = TextView(this)
            val s = Slider(this).apply { valueFrom = min.toFloat(); valueTo = max.toFloat(); stepSize = 1f; value = get().coerceIn(min, max).toFloat() }
            label.text = fmt(s.value.toInt())
            s.addOnChangeListener { _, v, _ -> label.text = fmt(v.toInt()) }
            val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(16), dp(24), 0); addView(label); addView(s) }
            MaterialAlertDialogBuilder(this).setTitle(title).setView(box)
                .setPositiveButton(android.R.string.ok) { _, _ -> set(s.value.toInt()); refreshers.forEach { it() } }
                .setNegativeButton(android.R.string.cancel, null).show()
        }, enabled)
    }

    private fun choiceRow(title: Int, options: List<Int>, get: () -> Int, set: (Int) -> Unit, enabled: () -> Boolean) {
        valueRow(title, { getString(options[get().coerceIn(0, options.size - 1)]) }, {
            MaterialAlertDialogBuilder(this).setTitle(title)
                .setSingleChoiceItems(options.map { getString(it) }.toTypedArray(), get()) { d, i -> set(i); d.dismiss(); refreshers.forEach { it() } }
                .setNegativeButton(android.R.string.cancel, null).show()
        }, enabled)
    }
}
