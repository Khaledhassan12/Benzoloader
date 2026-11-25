package khaled.benzo.ui.settings

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider

/**
 * Turns a (label, slider) pair into a settings row: the title, then the current value in the accent colour on a second line.
 * Tapping the row opens a dialog with a slider that has the same range and step. The original slider stays in the layout, hidden,
 * as the single place the value is stored, so saving code does not change.
 */
class ValueRow(
    private val context: Context,
    private val row: TextView,
    private val store: Slider,
    private val title: String,
    private val format: (Int) -> String,
    private val note: ((Int) -> String)? = null
) {
    init {
        store.visibility = View.GONE
        val d = context.resources.displayMetrics.density
        row.minHeight = (72 * d).toInt()
        row.gravity = android.view.Gravity.CENTER_VERTICAL
        row.setPadding((20 * d).toInt(), (12 * d).toInt(), (20 * d).toInt(), (12 * d).toInt())
        (row.layoutParams as? ViewGroup.MarginLayoutParams)?.setMargins(0, 0, 0, 0)
        val tv = TypedValue(); context.theme.resolveAttribute(android.R.attr.selectableItemBackground, tv, true)
        row.setBackgroundResource(tv.resourceId)
        row.isClickable = true
        row.setOnClickListener { openDialog() }
        store.addOnChangeListener { _, _, _ -> refresh() }
        refresh()
    }

    fun refresh() {
        val accent = MaterialColors.getColor(row, com.google.android.material.R.attr.colorPrimary)
        val b = SpannableStringBuilder(title).append('\n')
        val start = b.length
        b.append(format(store.value.toInt()))
        b.setSpan(AbsoluteSizeSpan(13, true), start, b.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        b.setSpan(ForegroundColorSpan(accent), start, b.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        b.setSpan(StyleSpan(Typeface.BOLD), start, b.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        row.text = b
    }

    private fun openDialog() {
        val d = context.resources.displayMetrics.density
        val box = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding((24 * d).toInt(), (16 * d).toInt(), (24 * d).toInt(), 0) }
        val valueText = TextView(context)
        val slider = Slider(context).apply {
            valueFrom = store.valueFrom; valueTo = store.valueTo; stepSize = store.stepSize; value = store.value
        }
        fun show(v: Int) { valueText.text = format(v) + (note?.let { "\n" + it(v) } ?: "") }
        show(slider.value.toInt())
        slider.addOnChangeListener { _, v, _ -> show(v.toInt()) }
        box.addView(valueText); box.addView(slider)
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setView(box)
            .setPositiveButton(android.R.string.ok) { _, _ -> store.value = slider.value }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
