package khaled.benzo.core.storage

/** What the check needs from the file system, so the rules can be tested without Android. */
interface FolderFs {
    fun exists(path: String): Boolean
    fun isDirectory(path: String): Boolean
    /** True only when a file really can be created in this existing folder (a probe file is created and removed). */
    fun canCreateFileIn(dir: String): Boolean
}

/**
 * Can downloads be written into this folder? Never changes the folder or creates it: the answer is about the folder as chosen.
 * A folder that does not exist yet is fine when the nearest existing parent accepts new files (the download will create the folder).
 */
object FolderCheck {
    enum class Verdict { OK, EMPTY, NOT_ABSOLUTE, NOT_A_FOLDER, NOT_WRITABLE }

    fun check(path: String, fs: FolderFs): Verdict {
        val p = path.trim()
        if (p.isEmpty()) return Verdict.EMPTY
        if (!p.startsWith("/")) return Verdict.NOT_ABSOLUTE
        var cur: String? = p.trimEnd('/').ifEmpty { "/" }
        while (cur != null && !fs.exists(cur)) cur = parentOf(cur)
        if (cur == null) return Verdict.NOT_WRITABLE
        if (!fs.isDirectory(cur)) return Verdict.NOT_A_FOLDER
        return if (fs.canCreateFileIn(cur)) Verdict.OK else Verdict.NOT_WRITABLE
    }

    fun parentOf(path: String): String? {
        if (path == "/") return null
        val i = path.lastIndexOf('/')
        return if (i <= 0) "/" else path.substring(0, i)
    }
}
