package khaled.benzo.ui.common

import android.app.Activity
import android.graphics.Color
import android.util.TypedValue
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import khaled.benzo.R

/**
 * Transparent status and navigation bars on every screen. The reference (read for behaviour): content runs under both
 * bars, which are tinted with the bar colour at low opacity (about 50% for light bars, 31% for dark ones) on phones.
 * The main screen lays itself out with fitsSystemWindows, so only the colours are set there; other screens get the
 * system-bar insets as padding so nothing hides under the bars.
 */
object EdgeToEdge {
    fun apply(activity: Activity) {
        val window = activity.window
        // Dialog-style screens (Editor, Properties, Torrent editor, Batch) float like the reference's dialogs and keep their window as is.
        if (window.isFloating) return
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val tv = TypedValue()
        val bar = if (activity.theme.resolveAttribute(R.attr.barBackground, tv, true) && tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT) tv.data
        else activity.theme.obtainStyledAttributes(intArrayOf(android.R.attr.colorBackground)).use { it.getColor(0, Color.BLACK) }
        val light = ColorUtils.calculateLuminance(bar) > 0.5
        val tint = ColorUtils.setAlphaComponent(bar, if (light) 128 else 80)
        window.statusBarColor = tint
        window.navigationBarColor = tint
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = light
            isAppearanceLightNavigationBars = light
        }
        if (activity is khaled.benzo.ui.main.MainActivity) return
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        ViewCompat.setOnApplyWindowInsetsListener(content) { v, insets ->
            val b = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(b.left, b.top, b.right, maxOf(b.bottom, ime.bottom))
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.requestApplyInsets(content)
    }
}
