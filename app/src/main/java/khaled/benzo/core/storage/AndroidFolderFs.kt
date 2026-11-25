package khaled.benzo.core.storage

import java.io.File

/** Real file system for [FolderCheck]. The probe file is created and deleted at once; it never stays in the user's folder. */
class AndroidFolderFs : FolderFs {
    override fun exists(path: String) = File(path).exists()
    override fun isDirectory(path: String) = File(path).isDirectory
    override fun canCreateFileIn(dir: String): Boolean = try {
        val probe = File(dir, ".benzoloader_probe_${System.nanoTime()}")
        val created = probe.createNewFile()
        if (created) probe.delete()
        created
    } catch (e: Exception) {
        false
    }
}
