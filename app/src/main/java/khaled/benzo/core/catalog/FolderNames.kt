package khaled.benzo.core.catalog

import khaled.benzo.data.model.FileCategory

/**
 * Folder name per category for cataloging. Defaults are built in; the user can override each one.
 * Stored as text, one "categoryKey=name" per line. A name is one folder level: it cannot contain a path separator,
 * cannot be "." or "..", and a blank or unusable name falls back to the default.
 */
class FolderNames(private val overrides: Map<FileCategory, String> = emptyMap()) {

    fun nameFor(category: FileCategory): String? {
        val default = defaults[category] ?: return null
        return overrides[category]?.let { clean(it) } ?: default
    }

    fun encode(): String = overrides.entries.joinToString("\n") { "${it.key.key}=${it.value}" }

    companion object {
        val defaults: Map<FileCategory, String> = mapOf(
            FileCategory.VIDEO to "Video",
            FileCategory.MUSIC to "Music",
            FileCategory.DOCUMENT to "Documents",
            FileCategory.ARCHIVE to "Archives",
            FileCategory.PROGRAM to "Programs",
            FileCategory.IMAGE to "Images",
            FileCategory.TORRENT to "Torrents",
            FileCategory.OTHER to "Other"
        )

        /** Categories the user can rename, in display order. */
        val editable: List<FileCategory> get() = defaults.keys.toList()

        fun clean(raw: String): String? {
            val n = raw.trim().replace(Regex("[\\u0000-\\u001f<>:\"|?*]"), "_")
            if (n.isEmpty() || n == "." || n == ".." || n.contains('/') || n.contains('\\')) return null
            return n.take(40)
        }

        fun decode(text: String?): FolderNames {
            if (text.isNullOrBlank()) return FolderNames()
            val map = LinkedHashMap<FileCategory, String>()
            for (line in text.lineSequence()) {
                val i = line.indexOf('=')
                if (i <= 0) continue
                val cat = FileCategory.entries.firstOrNull { it.key == line.substring(0, i) } ?: continue
                val name = clean(line.substring(i + 1)) ?: continue
                if (cat in defaults) map[cat] = name
            }
            return FolderNames(map)
        }

        /** Builds the stored text from user input; blank or unusable entries are left out (default is used). */
        fun fromInput(input: Map<FileCategory, String>): FolderNames =
            FolderNames(input.mapNotNull { (c, v) -> clean(v)?.takeIf { it != defaults[c] }?.let { c to it } }.toMap())
    }
}
