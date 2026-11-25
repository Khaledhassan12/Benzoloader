package khaled.benzo.core.files

/** Picks a file name that does not collide with an existing file or a queued download. */
object FileNaming {
    /**
     * "report.pdf" -> "report (1).pdf" -> "report (2).pdf" ...; "archive.tar.gz" keeps ".gz" as its extension.
     * [taken] answers whether a candidate name is already used in the target folder.
     */
    fun uniqueName(name: String, taken: (String) -> Boolean): String {
        if (!taken(name)) return name
        val dot = name.lastIndexOf('.')
        val hasExt = dot > 0 && dot < name.length - 1
        val base = if (hasExt) name.substring(0, dot) else name
        val ext = if (hasExt) name.substring(dot) else ""
        var i = 1
        while (i < 10_000) {
            val candidate = "$base ($i)$ext"
            if (!taken(candidate)) return candidate
            i++
        }
        return "$base (${System.currentTimeMillis()})$ext"
    }
}

/** What a download that failed for good (all retries used) becomes. */
object ErrorPolicy {
    /** Paused keeps the partial file and shows as resumable; error is the plain failed state. */
    fun statusAfterFailure(pauseAfterError: Boolean): khaled.benzo.data.model.DownloadStatus =
        if (pauseAfterError) khaled.benzo.data.model.DownloadStatus.PAUSED else khaled.benzo.data.model.DownloadStatus.ERROR
}
