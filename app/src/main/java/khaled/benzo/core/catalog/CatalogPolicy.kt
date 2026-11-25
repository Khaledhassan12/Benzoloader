package khaled.benzo.core.catalog

import khaled.benzo.data.model.FileCategory
import java.io.File

/** Sorts new downloads into per-type folders under the download root, only when the user did not pick another folder. */
object CatalogPolicy {

    fun folderName(category: FileCategory, names: FolderNames = FolderNames()): String? = names.nameFor(category)

    /**
     * @param chosenPath folder shown in the editor when the user pressed add
     * @param rootPath   the default download folder
     * Returns [chosenPath] unchanged when cataloging is off, the user picked a different folder, or the category has no folder.
     */
    fun targetFolder(chosenPath: String, rootPath: String, category: FileCategory, enabled: Boolean, names: FolderNames = FolderNames()): String {
        if (!enabled) return chosenPath
        val sub = folderName(category, names) ?: return chosenPath
        if (File(chosenPath).path.trimEnd('/') != File(rootPath).path.trimEnd('/')) return chosenPath
        return File(rootPath, sub).path
    }
}
