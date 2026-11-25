package khaled.benzo.ui.common

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import com.google.android.material.color.MaterialColors
import com.google.android.material.switchmaterial.SwitchMaterial
import khaled.benzo.R

/**
 * Settings row: a title, an optional smaller summary line under it, and the switch at the end.
 * The summary comes from the `prefSummary` attribute; the title is the normal text.
 */
class PrefSwitch @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = androidx.appcompat.R.attr.switchStyle
) : SwitchMaterial(context, attrs, defStyle) {

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.PrefSwitch)
        val summary = a.getString(R.styleable.PrefSwitch_prefSummary)
        a.recycle()
        if (!summary.isNullOrEmpty()) setRow(text?.toString().orEmpty(), summary)
    }

    fun setRow(title: String, summary: String) {
        val grey = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant)
        val b = SpannableStringBuilder(title).append('\n')
        val start = b.length
        b.append(summary)
        b.setSpan(AbsoluteSizeSpan(13, true), start, b.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        b.setSpan(ForegroundColorSpan(grey), start, b.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        text = b
    }
}
