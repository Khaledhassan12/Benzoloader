package khaled.benzo.core.storage

/**
 * Android 8 to 10 (API 26 to 29): writing to shared folders needs the WRITE_EXTERNAL_STORAGE runtime permission, which the app
 * declares for API 29 and below. From Android 11 the permission adds nothing, so nothing is asked there. Pure logic.
 * Source: developer.android.com/training/data-storage/use-cases ("Running on Android 9 or lower": request WRITE_EXTERNAL_STORAGE).
 */
object LegacyStoragePolicy {
    const val LAST_SDK = 29

    enum class Step { NOTHING, ASK, EXPLAIN_THEN_ASK }

    fun isApplicable(sdk: Int): Boolean = sdk <= LAST_SDK

    fun next(sdk: Int, granted: Boolean, shouldShowRationale: Boolean): Step = when {
        !isApplicable(sdk) || granted -> Step.NOTHING
        shouldShowRationale -> Step.EXPLAIN_THEN_ASK
        else -> Step.ASK
    }

    /** Denied and the system will not ask again: offer the app settings page. */
    fun offerSettings(granted: Boolean, shouldShowRationale: Boolean) = !granted && !shouldShowRationale

    fun offerInFolderDialog(sdk: Int, granted: Boolean) = isApplicable(sdk) && !granted
}
