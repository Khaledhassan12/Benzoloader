package khaled.benzo.service

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import khaled.benzo.core.overlay.TopWindowConfig
import khaled.benzo.core.overlay.TopWindowSample
import khaled.benzo.core.overlay.TopWindowSpec

/**
 * Transparent progress strip drawn above other windows (the Top window setting). It exists only while the setting is on,
 * the "draw over other apps" permission is granted and a transfer is running. Not touch-interactive. WRITTEN, UNBUILT, never run.
 */
class TopWindowOverlay(private val context: Context) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: TextView? = null
    private var lastKey: TopWindowConfig? = null

    fun canDraw(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

    /** Call on every progress tick. Adds, updates or removes the strip as [TopWindowSpec.shouldShow] says. */
    fun update(config: TopWindowConfig, runningCount: Int, sample: TopWindowSample) {
        if (!TopWindowSpec.shouldShow(config, canDraw(), runningCount)) { hide(); return }
        val v = view ?: TextView(context).also { view = it }
        if (lastKey != config || v.parent == null) {
            style(v, config)
            if (v.parent != null) wm.updateViewLayout(v, params(config)) else runCatching { wm.addView(v, params(config)) }
            lastKey = config
        }
        v.text = TopWindowSpec.text(config, sample)
        v.background.level = TopWindowSpec.percent(sample) * 100
    }

    fun hide() {
        val v = view ?: return
        runCatching { if (v.parent != null) wm.removeView(v) }
        view = null; lastKey = null
    }

    private fun style(v: TextView, c: TopWindowConfig) {
        v.setTextColor(c.textColor)
        v.setTextSize(TypedValue.COMPLEX_UNIT_PX, c.textSizePx.toFloat())
        v.gravity = Gravity.CENTER_VERTICAL or Gravity.START
        v.setPadding(8, 0, 8, 0)
        v.maxLines = 1
        v.isClickable = false
        val bar = ClipDrawable(ColorDrawable(c.barColor), Gravity.START, ClipDrawable.HORIZONTAL)
        v.background = LayerDrawable(arrayOf(ColorDrawable(c.backgroundColor), bar))
        v.alpha = TopWindowSpec.alphaFraction(c.opacityPercent)
        // Level drives the clip; the layer's own level is set on each update.
        v.visibility = View.VISIBLE
    }

    private fun params(c: TopWindowConfig): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        val w = if (c.widthPx > 0) c.widthPx else WindowManager.LayoutParams.MATCH_PARENT
        return WindowManager.LayoutParams(
            w, c.lineHeightPx.coerceAtLeast(1), type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = (when (c.horizontal) {
                TopWindowConfig.Align.START -> Gravity.START
                TopWindowConfig.Align.CENTER -> Gravity.CENTER_HORIZONTAL
                TopWindowConfig.Align.END -> Gravity.END
            }) or (when (c.vertical) {
                TopWindowConfig.VAlign.TOP -> Gravity.TOP
                TopWindowConfig.VAlign.CENTER -> Gravity.CENTER_VERTICAL
                TopWindowConfig.VAlign.BOTTOM -> Gravity.BOTTOM
            })
            x = c.offsetXPx; y = c.offsetYPx
        }
    }
}
