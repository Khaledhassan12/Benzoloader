package khaled.benzo.data.model

enum class DownloadStatus(val code: Int) {
    QUEUED(0),
    CONNECTING(1),
    RUNNING(2),
    PAUSED(3),
    COMPLETED(4),
    ERROR(5);

    companion object {
        fun fromCode(code: Int): DownloadStatus {
            return entries.firstOrNull { it.code == code } ?: QUEUED
        }
    }
}
