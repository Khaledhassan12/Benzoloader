package khaled.benzo.core.data

import khaled.benzo.core.catalog.CatalogPolicy
import khaled.benzo.core.catalog.FolderNames
import khaled.benzo.core.files.FileNaming
import khaled.benzo.data.model.FileCategory

/** One download to create from an imported entry. */
data class PlannedDownload(val url: String, val fileName: String, val folder: String, val category: FileCategory)

/**
 * Decides, without touching Android or the database, which imported entries become downloads and where they go.
 * [folderExists] answers whether a file name already exists on disk in a folder (a fake in tests).
 */
class ImportPlanner(
    private val root: String,
    private val catalogEnabled: Boolean,
    private val names: FolderNames,
    private val folderExists: (folder: String, name: String) -> Boolean
) {
    fun plan(entries: List<ImportEntry>, knownUrls: Set<String>, takenNames: Set<Pair<String, String>>): List<PlannedDownload> {
        val urls = knownUrls.toMutableSet()
        val taken = takenNames.toMutableSet()
        val out = ArrayList<PlannedDownload>()
        for (e in entries) {
            if (e.url in urls) continue
            val raw = ListTransfer.nameFor(e)
            val category = FileCategory.fromExtension(raw)
            val folder = CatalogPolicy.targetFolder(root, root, category, catalogEnabled, names)
            val name = FileNaming.uniqueName(raw) { c -> (folder to c) in taken || folderExists(folder, c) }
            taken.add(folder to name)
            urls.add(e.url)
            out.add(PlannedDownload(e.url, name, folder, category))
        }
        return out
    }
}
