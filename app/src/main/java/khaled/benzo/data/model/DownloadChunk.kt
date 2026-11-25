package khaled.benzo.data.model

data class DownloadChunk(
    val chunkId: Long = 0,
    val downloadId: Long,
    val partIndex: Int,
    val startByte: Long,
    var currentByte: Long,
    val endByte: Long,
    var status: Int = 0 // 0: Pending, 1: Running, 2: Completed, 3: Error
) {
    /**
     * Highest position known to be flushed to disk (-1: not tracked yet). Only this is saved to the database while a
     * download runs, so the saved progress is never ahead of what a successful flush covered. Not part of equals/copy.
     */
    @Volatile var durableByte: Long = -1L

    /** The position to save: the flushed one when tracked, otherwise the live one. */
    fun persistedByte(): Long = if (durableByte in startByte..currentByte) durableByte else currentByte

    val totalSize: Long
        get() = if (endByte >= startByte) (endByte - startByte) + 1 else 0L

    val downloadedSize: Long
        get() = if (currentByte >= startByte) (currentByte - startByte) else 0L

    val isFinished: Boolean
        get() = currentByte > endByte && endByte > 0
}
