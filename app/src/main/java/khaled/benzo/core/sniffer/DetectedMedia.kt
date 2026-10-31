package khaled.benzo.core.sniffer

data class DetectedMedia(
    val url: String,
    val title: String,
    val format: String,
    val mimeType: String,
    val sizeBytes: Long = -1L
) {
    val displayTitle: String
        get() = if (title.isNotBlank()) title else "Media_$format"
}
