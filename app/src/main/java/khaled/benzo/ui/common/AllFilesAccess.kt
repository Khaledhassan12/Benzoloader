package khaled.benzo.ui.common

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import khaled.benzo.R
import khaled.benzo.core.storage.AllFilesAccessPolicy
import khaled.benzo.data.repository.SettingsRepository

/**
 * The optional all-files access request (GitHub APK variant). It explains why, lets the user decline (and remembers that),
 * and opens the system screen where the user decides. Nothing is granted by the app. Always re-check with [isGranted] on return.
 * A store variant without this access can leave this class out: nothing else depends on it except two calls from the editor and Settings.
 */
object AllFilesAccess {
    fun isApplicable(): Boolean = AllFilesAccessPolicy.isApplicable(Build.VERSION.SDK_INT)

    /** Below Android 11 the question does not exist, so this returns true. */
    fun isGranted(): Boolean = !isApplicable() || Environment.isExternalStorageManager()

    fun shouldOfferInDialog(activity: AppCompatActivity): Boolean =
        AllFilesAccessPolicy.offerInFolderDialog(Build.VERSION.SDK_INT, isGranted(), SettingsRepository(activity).allFilesDeclined)

    /** Explains, then opens the system screen if the user agrees. */
    fun explainAndRequest(activity: AppCompatActivity) {
        AlertDialog.Builder(activity)
            .setTitle(R.string.allfiles_title)
            .setMessage(R.string.allfiles_message)
            .setPositiveButton(R.string.allfiles_open) { _, _ -> openSystemScreen(activity) }
            .setNegativeButton(R.string.allfiles_decline) { _, _ -> SettingsRepository(activity).allFilesDeclined = true }
            .show()
    }

    private fun openSystemScreen(activity: AppCompatActivity) {
        try {
            activity.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(activity, R.string.allfiles_unavailable, Toast.LENGTH_LONG).show()
        }
    }
}
