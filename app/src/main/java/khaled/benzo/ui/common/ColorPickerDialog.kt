package khaled.benzo.ui.common

import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.widget.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import khaled.benzo.R
import khaled.benzo.core.ui.ColorMath

/**
 * Shared colour picker (written, unbuilt, unrendered). Layout follows the reference capture (video 5, about 330 s):
 * title, saturation/brightness square with a ring marker, vertical hue bar, alpha slider, then hex box, old swatch,
 * arrow, new swatch, hex box, and the buttons Presets, Cancel, OK. Drawing code and wording are my own.
 * NOT SEEN: what the reference Presets button opens. Here it lists [default] (when given) and a small palette I chose.
 */
object ColorPickerDialog {
    private class SvSquare(context: Context, private val onChange: (Float, Float) -> Unit) : View(context) {
        var hue = 0f; set(v) { field = v; invalidate() }
        var sat = 0f; var value = 0f
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f * resources.displayMetrics.density; color = Color.WHITE }
        override fun onDraw(c: Canvas) {
            val w = width.toFloat(); val h = height.toFloat()
            paint.shader = ComposeShader(
                LinearGradient(0f, 0f, w, 0f, Color.WHITE, ColorMath.toArgb(255, hue, 1f, 1f), Shader.TileMode.CLAMP),
                LinearGradient(0f, 0f, 0f, h, Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP), PorterDuff.Mode.MULTIPLY)
            c.drawRect(0f, 0f, w, h, paint)
            c.drawCircle(sat * w, (1f - value) * h, 8f * resources.displayMetrics.density, ring)
        }
        override fun onTouchEvent(e: MotionEvent): Boolean {
            parent?.requestDisallowInterceptTouchEvent(true)
            sat = (e.x / width).coerceIn(0f, 1f); value = 1f - (e.y / height).coerceIn(0f, 1f)
            invalidate(); onChange(sat, value); return true
        }
    }

    private class HueBar(context: Context, private val onChange: (Float) -> Unit) : View(context) {
        var hue = 0f; set(v) { field = v; invalidate() }
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val mark = Paint().apply { color = Color.WHITE; strokeWidth = 3f * resources.displayMetrics.density }
        override fun onDraw(c: Canvas) {
            val colors = IntArray(7) { ColorMath.toArgb(255, it * 60f, 1f, 1f) }
            paint.shader = LinearGradient(0f, 0f, 0f, height.toFloat(), colors, null, Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            val y = hue / 360f * height
            c.drawLine(0f, y, width.toFloat(), y, mark)
        }
        override fun onTouchEvent(e: MotionEvent): Boolean {
            parent?.requestDisallowInterceptTouchEvent(true)
            hue = (e.y / height).coerceIn(0f, 1f) * 360f; onChange(hue); return true
        }
    }

    private val PALETTE = intArrayOf(
        0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFFE53935.toInt(), 0xFFFB8C00.toInt(), 0xFFFDD835.toInt(), 0xFF43A047.toInt(),
        0xFF00ACC1.toInt(), 0xFF1E88E5.toInt(), 0xFF5E35B1.toInt(), 0xFFD81B60.toInt(), 0xFF757575.toInt(), 0xFFCCCCCC.toInt())

    private fun swatch(color: Int, dp: Float) = GradientDrawable().apply { setColor(color); setStroke((dp).toInt().coerceAtLeast(1), 0x66808080) }

    /** [default] non-null adds a "Default" entry to Presets that picks that colour. [onPicked] runs only on OK with valid hex. */
    fun show(context: Context, title: Int, initial: Int, default: Int?, onPicked: (Int) -> Unit) {
        val d = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        var alpha = ColorMath.alphaOf(initial)
        val hsv = ColorMath.toHsv(initial)
        var hue = hsv.h; var sat = hsv.s; var value = hsv.v
        var current = initial
        lateinit var newSwatch: View
        lateinit var hexNew: EditText
        lateinit var sv: SvSquare
        lateinit var bar: HueBar
        lateinit var alphaSlider: Slider
        var updating = false

        fun refresh(fromHex: Boolean) {
            updating = true
            current = ColorMath.toArgb(alpha, hue, sat, value)
            newSwatch.background = swatch(current, d)
            if (!fromHex) hexNew.setText(ColorMath.format(current))
            updating = false
        }
        sv = SvSquare(context) { s, v -> sat = s; value = v; refresh(false) }.also { it.hue = hue; it.sat = sat; it.value = value }
        bar = HueBar(context) { h -> hue = h; sv.hue = h; refresh(false) }.also { it.hue = hue }
        alphaSlider = Slider(context).apply { valueFrom = 0f; valueTo = 255f; value = alpha.toFloat(); addOnChangeListener { _, v, fromUser -> if (fromUser) { alpha = v.toInt(); refresh(false) } } }
        val hexOld = TextView(context).apply { text = ColorMath.format(initial); textSize = 14f }
        hexNew = EditText(context).apply {
            setText(ColorMath.format(initial)); textSize = 14f; maxLines = 1
            filters = arrayOf(InputFilter.AllCaps(), InputFilter.LengthFilter(8))
            minWidth = dp(88)
        }
        val oldSwatch = View(context).apply { background = swatch(initial, d) }
        newSwatch = View(context).apply { background = swatch(initial, d) }
        hexNew.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (updating) return
                val parsed = ColorMath.parse(s.toString())?.takeIf { s.toString().length == 8 } ?: return
                val h = ColorMath.toHsv(parsed)
                alpha = ColorMath.alphaOf(parsed); hue = h.h; sat = h.s; value = h.v
                sv.hue = hue; sv.sat = sat; sv.value = value; sv.invalidate(); bar.hue = hue; alphaSlider.value = alpha.toFloat()
                refresh(true)
            }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        val squareRow = LinearLayout(context).apply {
            addView(sv, LinearLayout.LayoutParams(0, dp(220), 1f))
            addView(bar, LinearLayout.LayoutParams(dp(32), dp(220)).apply { marginStart = dp(8) })
        }
        val hexRow = LinearLayout(context).apply {
            gravity = android.view.Gravity.CENTER_VERTICAL
            addView(hexOld); addView(oldSwatch, LinearLayout.LayoutParams(dp(40), dp(32)).apply { marginStart = dp(8) })
            addView(TextView(context).apply { text = "\u2192"; textSize = 20f; setPadding(dp(8), 0, dp(8), 0) })
            addView(newSwatch, LinearLayout.LayoutParams(dp(40), dp(32))); addView(hexNew, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(8) })
        }
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(16), dp(24), 0)
            addView(squareRow); addView(alphaSlider); addView(hexRow)
        }
        val dialog = MaterialAlertDialogBuilder(context).setTitle(title).setView(ScrollView(context).apply { addView(box) })
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.color_presets, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                // A half-typed hex box is not applied silently: tell the user and keep the dialog open.
                if (hexNew.text.length != 8 || ColorMath.parse(hexNew.text.toString()) == null) {
                    hexNew.error = context.getString(R.string.color_hex_invalid); return@setOnClickListener
                }
                onPicked(current); dialog.dismiss()
            }
            dialog.getButton(android.content.DialogInterface.BUTTON_NEUTRAL).setOnClickListener {
                val names = ArrayList<String>(); val values = ArrayList<Int>()
                if (default != null) { names += context.getString(R.string.color_default) + "  " + ColorMath.format(default); values += default }
                PALETTE.forEach { names += ColorMath.format(it); values += it }
                MaterialAlertDialogBuilder(context).setTitle(R.string.color_presets)
                    .setItems(names.toTypedArray()) { _, which -> hexNew.setText(ColorMath.format(values[which])) }
                    .setNegativeButton(android.R.string.cancel, null).show()
            }
        }
        dialog.show()
    }
}
