package khaled.benzo.ui.common

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
import khaled.benzo.core.storage.LegacyStoragePolicy

/**
 * The WRITE_EXTERNAL_STORAGE runtime request for Android 8 to 10 (both distributions: it is a normal permission).
 * The grant is read from the system each time, never cached. Create as a property of the activity. [onGranted] runs after a grant.
 */
class LegacyStoragePermissionFlow(private val activity: ComponentActivity, private val onGranted: () -> Unit) {
    private val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE

    private val launcher = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) onGranted() else if (LegacyStoragePolicy.offerSettings(granted(), rationale())) offerSettings()
    }

    fun granted() = !LegacyStoragePolicy.isApplicable(Build.VERSION.SDK_INT) ||
        ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED

    private fun rationale() = activity.shouldShowRequestPermissionRationale(permission)

    fun offerInDialog() = LegacyStoragePolicy.offerInFolderDialog(Build.VERSION.SDK_INT, granted())

    fun start() {
        when (LegacyStoragePolicy.next(Build.VERSION.SDK_INT, granted(), rationale())) {
            LegacyStoragePolicy.Step.NOTHING -> Unit
            LegacyStoragePolicy.Step.ASK -> launcher.launch(permission)
            LegacyStoragePolicy.Step.EXPLAIN_THEN_ASK -> AlertDialog.Builder(activity)
                .setMessage(R.string.legacy_storage_why)
                .setPositiveButton(android.R.string.ok) { _, _ -> launcher.launch(permission) }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun offerSettings() {
        AlertDialog.Builder(activity)
            .setMessage(R.string.legacy_storage_settings)
            .setPositiveButton(R.string.notif_open_settings) { _, _ ->
                activity.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", activity.packageName, null)))
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
