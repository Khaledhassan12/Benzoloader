package khaled.benzo.ui.common

import androidx.appcompat.app.AppCompatActivity

/**
 * Play variant: no all-files access. The manifest of this variant does not declare MANAGE_EXTERNAL_STORAGE, nothing here opens the
 * system screen, and the editor and Settings show no such option. Same API as the GitHub variant so shared code compiles for both.
 */
object AllFilesAccess {
    fun isApplicable(): Boolean = false
    fun isGranted(): Boolean = true
    @Suppress("UNUSED_PARAMETER")
    fun shouldOfferInDialog(activity: AppCompatActivity): Boolean = false
    @Suppress("UNUSED_PARAMETER")
    fun explainAndRequest(activity: AppCompatActivity) = Unit
}
