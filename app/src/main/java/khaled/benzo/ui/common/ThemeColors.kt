package khaled.benzo.ui.common

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import com.google.android.material.color.DynamicColors
import khaled.benzo.R
import khaled.benzo.data.repository.SettingsRepository

/**
 * Applies the chosen colour source to every activity. 0 = device colours through Material's DynamicColors (only on
 * Android 12+; on older versions nothing is applied and the green Benzo theme stays). 1 = the Benzo green theme.
 * 2..4 = a preset overlay that replaces the primary colours, on every version. Read at activity creation, so a change
 * shows on the next screen that opens (the settings screen recreates itself).
 */
object ThemeColors {
    fun overlayFor(choice: Int): Int? = when (choice) {
        2 -> R.style.ThemeOverlay_Benzo_Blue
        3 -> R.style.ThemeOverlay_Benzo_Purple
        4 -> R.style.ThemeOverlay_Benzo_Orange
        else -> null
    }

    fun install(app: Application) {
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) = apply(activity)
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                // Before Android 10 there is no pre-created callback; this one runs before setContentView in our activities.
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) apply(activity)
            }
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    private fun apply(activity: Activity) {
        val choice = SettingsRepository(activity).themeColor
        if (choice == 0) {
            if (DynamicColors.isDynamicColorAvailable()) DynamicColors.applyToActivityIfAvailable(activity)
            return
        }
        overlayFor(choice)?.let { activity.theme.applyStyle(it, true) }
    }
}
