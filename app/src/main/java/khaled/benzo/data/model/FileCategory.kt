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
    IMAGE("image"),
    OTHER("other"),
    TORRENT("torrent"),
    TRASH("trash");

    companion object {
        fun fromExtension(ext: String?): FileCategory {
            if (ext.isNullOrBlank()) return OTHER
            // Callers pass either an extension ("mp4", ".mp4") or a whole file name ("clip.mp4").
            val lower = ext.lowercase(Locale.ROOT).trim()
            val clean = if (lower.contains('.')) lower.substringAfterLast('.') else lower
            return when (clean) {
                "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "3gp", "ts", "m3u8", "mpd" -> VIDEO
                "mp3", "m4a", "flac", "wav", "aac", "ogg", "wma", "opus" -> MUSIC
                "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "epub" -> DOCUMENT
                "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "iso" -> ARCHIVE
                "apk", "xapk", "apks", "exe", "msi", "dmg", "deb", "rpm" -> PROGRAM
                "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "svg", "tiff" -> IMAGE
                "torrent" -> TORRENT
                else -> OTHER
            }
        }

        val IMAGES: FileCategory get() = IMAGE
        val AUDIO: FileCategory get() = MUSIC
        val DOCUMENTS: FileCategory get() = DOCUMENT
        val ARCHIVES: FileCategory get() = ARCHIVE
        val PROGRAMS: FileCategory get() = PROGRAM
    }
}
