package khaled.benzo.data.model

import java.util.Locale

enum class FileCategory(val key: String) {
    ALL("all"),
    DOWNLOADING("downloading"),
    COMPLETED("completed"),
    QUEUED("queued"),
    VIDEO("video"),
    MUSIC("music"),
    DOCUMENT("document"),
    ARCHIVE("archive"),
    PROGRAM("program"),
    TORRENT("torrent"),
    TRASH("trash");

    companion object {
        fun fromExtension(ext: String?): FileCategory {
            if (ext.isNullOrBlank()) return DOCUMENT
            val clean = ext.lowercase(Locale.ROOT).trimStart('.')
            return when (clean) {
                "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "3gp", "ts", "m3u8", "mpd" -> VIDEO
                "mp3", "m4a", "flac", "wav", "aac", "ogg", "wma", "opus" -> MUSIC
                "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "epub" -> DOCUMENT
                "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "iso" -> ARCHIVE
                "apk", "xapk", "apks", "exe", "msi", "dmg", "deb", "rpm" -> PROGRAM
                "torrent" -> TORRENT
                else -> DOCUMENT
            }
        }

        val AUDIO: FileCategory get() = MUSIC
        val DOCUMENTS: FileCategory get() = DOCUMENT
        val ARCHIVES: FileCategory get() = ARCHIVE
        val PROGRAMS: FileCategory get() = PROGRAM
    }
}
