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
        /** The built-in extension lists, one place for both sorting and the Cataloging screen. Order inside a list is display order. */
        val EXTENSIONS: Map<FileCategory, List<String>> = linkedMapOf(
            VIDEO to listOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "3gp", "ts", "m3u8", "mpd"),
            MUSIC to listOf("mp3", "m4a", "flac", "wav", "aac", "ogg", "wma", "opus"),
            DOCUMENT to listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "epub"),
            ARCHIVE to listOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "iso"),
            PROGRAM to listOf("apk", "xapk", "apks", "exe", "msi", "dmg", "deb", "rpm"),
            IMAGE to listOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "svg", "tiff"),
            TORRENT to listOf("torrent")
        )

        private val EXTENSION_TO_CATEGORY: Map<String, FileCategory> =
            EXTENSIONS.flatMap { (cat, exts) -> exts.map { it to cat } }.toMap()

        fun fromExtension(ext: String?): FileCategory {
            if (ext.isNullOrBlank()) return OTHER
            // Callers pass either an extension ("mp4", ".mp4") or a whole file name ("clip.mp4").
            val lower = ext.lowercase(Locale.ROOT).trim()
            val clean = if (lower.contains('.')) lower.substringAfterLast('.') else lower
            return EXTENSION_TO_CATEGORY[clean] ?: OTHER
        }

        val IMAGES: FileCategory get() = IMAGE
        val AUDIO: FileCategory get() = MUSIC
        val DOCUMENTS: FileCategory get() = DOCUMENT
        val ARCHIVES: FileCategory get() = ARCHIVE
        val PROGRAMS: FileCategory get() = PROGRAM
    }
}
