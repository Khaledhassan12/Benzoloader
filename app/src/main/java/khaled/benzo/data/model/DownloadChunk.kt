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
    val totalSize: Long
        get() = if (endByte >= startByte) (endByte - startByte) + 1 else 0L

    val downloadedSize: Long
        get() = if (currentByte >= startByte) (currentByte - startByte) else 0L

    val isFinished: Boolean
        get() = currentByte > endByte && endByte > 0
}
