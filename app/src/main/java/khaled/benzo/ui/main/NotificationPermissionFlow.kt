package khaled.benzo.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import khaled.benzo.R

/** What to do about the notification permission right now. Pure, so it can be unit-tested. */
enum class NotificationStep { NOTHING, EXPLAIN_THEN_ASK, ASK }

object NotificationPermissionPolicy {
    fun next(sdk: Int, granted: Boolean, shouldShowRationale: Boolean): NotificationStep = when {
        sdk < 33 || granted -> NotificationStep.NOTHING
        shouldShowRationale -> NotificationStep.EXPLAIN_THEN_ASK
        else -> NotificationStep.ASK
    }

    /** After a denial with no rationale left the system will not show the prompt again: offer app settings instead. */
    fun offerSettings(granted: Boolean, shouldShowRationale: Boolean) = !granted && !shouldShowRationale
}

/**
 * Android 13+ hides all notifications (including the download progress notification) until POST_NOTIFICATIONS is
 * granted. The state is read from the system each time, never cached. Call [start] once per fresh launch.
 * Register this object as a property so the launcher is registered before the activity is started.
 */
class NotificationPermissionFlow(private val activity: ComponentActivity) {
    private val permission = Manifest.permission.POST_NOTIFICATIONS

    private val launcher = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (NotificationPermissionPolicy.offerSettings(granted, rationale())) offerSettings()
    }

    private fun granted() = ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
    private fun rationale() = activity.shouldShowRequestPermissionRationale(permission)

    fun start() {
        when (NotificationPermissionPolicy.next(Build.VERSION.SDK_INT, granted(), rationale())) {
            NotificationStep.NOTHING -> Unit
            NotificationStep.ASK -> launcher.launch(permission)
            NotificationStep.EXPLAIN_THEN_ASK -> AlertDialog.Builder(activity)
                .setMessage(R.string.notif_permission_why)
                .setPositiveButton(android.R.string.ok) { _, _ -> launcher.launch(permission) }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun offerSettings() {
        AlertDialog.Builder(activity)
            .setMessage(R.string.notif_permission_settings)
            .setPositiveButton(R.string.notif_open_settings) { _, _ ->
                activity.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName))
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
