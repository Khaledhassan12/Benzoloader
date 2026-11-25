package khaled.benzo.core.queue

import khaled.benzo.data.model.DownloadStatus

/** What the editor does with an incoming link under the two quick-add switches. Pure, so it can be unit-tested. */
data class QuickAddDecision(val skipEditor: Boolean, val startNow: Boolean)

object QuickAddPolicy {
    /**
     * [external] is true only for a link opened or shared from another app. The start switch counts only while the
     * skip-editor switch is on, as in the reference (the child row is disabled while the parent is off).
     */
    fun decide(skipEditorSetting: Boolean, startAfterAddingSetting: Boolean, external: Boolean): QuickAddDecision {
        val skip = skipEditorSetting && external
        return QuickAddDecision(skipEditor = skip, startNow = skip && startAfterAddingSetting)
    }

    /**
     * Status a new download is stored with. Not starting means PAUSED: the queue scheduler only starts QUEUED items, so
     * nothing starts until the user resumes it (or uses Resume all). Starting means QUEUED, and the queue rules then apply.
     */
    fun initialStatus(startNow: Boolean): DownloadStatus = if (startNow) DownloadStatus.QUEUED else DownloadStatus.PAUSED
}
