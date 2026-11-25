package khaled.benzo.core.catalog

import khaled.benzo.data.model.FileCategory
import java.io.File

/** Sorts new downloads into per-type folders under the download root, only when the user did not pick another folder. */
object CatalogPolicy {

    fun folderName(category: FileCategory): String? = when (category) {
        FileCategory.VIDEO -> "Video"
        FileCategory.MUSIC -> "Music"
        FileCategory.DOCUMENT -> "Documents"
        FileCategory.ARCHIVE -> "Archives"
        FileCategory.PROGRAM -> "Programs"
        FileCategory.TORRENT -> "Torrents"
        else -> null
    }

    /**
     * @param chosenPath folder shown in the editor when the user pressed add
     * @param rootPath   the default download folder
     * Returns [chosenPath] unchanged when cataloging is off, the user picked a different folder, or the category has no folder.
     */
    fun targetFolder(chosenPath: String, rootPath: String, category: FileCategory, enabled: Boolean): String {
        if (!enabled) return chosenPath
        val sub = folderName(category) ?: return chosenPath
        if (File(chosenPath).path.trimEnd('/') != File(rootPath).path.trimEnd('/')) return chosenPath
        return File(rootPath, sub).path
    }
}
