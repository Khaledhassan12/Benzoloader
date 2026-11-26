package khaled.benzo.ui.settings

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import khaled.benzo.R
import khaled.benzo.data.repository.SettingsRepository
import khaled.benzo.ui.common.ColorPickerDialog

/**
 * Colour rows of the Interface screen: filename, information, progress bar and its background, list background.
 * Each row shows the hex value and a swatch, and a cross while a colour is set (OBSERVED, video 5). A stored 0 means
 * "stock colour", shown as the stock swatch. Not built here: Accent, Background of bars, Colour of creating file, and the
 * Individual settings gate (its effect was not seen). Unrendered.
 */
class InterfaceColorsActivity : AppCompatActivity() {
    private lateinit var repo: SettingsRepository
    private lateinit var list: LinearLayout
    private val refreshers = mutableListOf<() -> Unit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = SettingsRepository(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(com.google.android.material.appbar.MaterialToolbar(this).apply {
            setTitle(R.string.colors_title)
            setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
            setNavigationOnClickListener { finish() }
        }, LinearLayout.LayoutParams(-1, dp(56)))
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(list) }, LinearLayout.LayoutParams(-1, -1))
        setContentView(root)
        val stockText = TextView(this).currentTextColor
        row(R.string.col_filename, { repo.uiColFilename }, { repo.uiColFilename = it }, stockText)
        row(R.string.col_info, { repo.uiColInfo }, { repo.uiColInfo = it }, stockText)
        row(R.string.col_progress_bg, { repo.uiColProgressBg }, { repo.uiColProgressBg = it }, 0xFF9E9E9E.toInt())
        row(R.string.col_progress, { repo.uiColProgress }, { repo.uiColProgress = it }, 0xFF2E7D32.toInt())
        row(R.string.col_list_bg, { repo.uiColList }, { repo.uiColList = it }, 0xFF000000.toInt())
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun row(title: Int, get: () -> Int, set: (Int) -> Unit, shownWhenUnset: Int) {
        val text = TextView(this).apply {
            minHeight = dp(72); gravity = Gravity.CENTER_VERTICAL; setPadding(dp(20), dp(12), dp(20), dp(12)); isClickable = true
            val t = android.util.TypedValue(); theme.resolveAttribute(android.R.attr.selectableItemBackground, t, true); setBackgroundResource(t.resourceId)
        }
        val shown = { if (get() != 0) get() else shownWhenUnset }
        text.setOnClickListener {
            // The picker's Default entry hands back the default value; for these rows the default is "not set".
            ColorPickerDialog.show(this, title, shown(), shownWhenUnset) { picked -> set(if (picked == shownWhenUnset) 0 else picked); refreshers.forEach { it() } }
        }
        val cross = android.widget.ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setBackgroundResource(android.R.color.transparent)
            contentDescription = getString(R.string.top_color_reset)
            setOnClickListener { set(0); refreshers.forEach { it() } }
        }
        val square = View(this).apply { setOnClickListener { text.performClick() } }
        val wrap = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        wrap.addView(text, LinearLayout.LayoutParams(0, -2, 1f))
        wrap.addView(cross, LinearLayout.LayoutParams(dp(40), dp(40)))
        wrap.addView(square, LinearLayout.LayoutParams(dp(28), dp(28)).apply { marginStart = dp(4); marginEnd = dp(20) })
        list.addView(wrap)
        val paint = {
            text.text = getString(title) + "\n" + (if (get() == 0) getString(R.string.col_stock) else "#%08X".format(get()))
            square.background = android.graphics.drawable.GradientDrawable().apply { setColor(shown()); setStroke(dp(1), 0x66808080) }
            cross.visibility = if (get() != 0) View.VISIBLE else View.INVISIBLE
        }
        paint(); refreshers += paint
    }
}
