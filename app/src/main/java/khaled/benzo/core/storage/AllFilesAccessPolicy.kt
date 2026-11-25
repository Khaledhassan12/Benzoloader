package khaled.benzo.core.storage

/**
 * When to offer the optional "all files access" request. It is an offer, never forced: the user can decline and keep using the app.
 * Pure logic. [sdk] is Build.VERSION.SDK_INT. The special access exists from Android 11 (API 30).
 */
object AllFilesAccessPolicy {
    const val FIRST_SDK_WITH_ACCESS = 30

    fun isApplicable(sdk: Int): Boolean = sdk >= FIRST_SDK_WITH_ACCESS

    /** Offer it inside a folder problem dialog: applicable, not granted yet, and the user did not decline before. */
    fun offerInFolderDialog(sdk: Int, granted: Boolean, declinedBefore: Boolean): Boolean =
        isApplicable(sdk) && !granted && !declinedBefore
}
