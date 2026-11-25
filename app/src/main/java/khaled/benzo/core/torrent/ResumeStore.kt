package khaled.benzo.core.torrent

import java.io.File
import java.io.IOException

/**
 * Keeps the libtorrent resume data of one torrent in one file, so a restart can continue from the saved piece state
 * instead of re-checking every file. The data is only a hint: a missing, empty, oversized or unreadable file means
 * "no resume data" and the torrent is added the normal way. Writes go to a temp file first, then rename.
 */
object ResumeStore {
    const val MAX_BYTES = 4 * 1024 * 1024

    fun fileFor(dir: File, infoHash: String): File = File(dir, "$infoHash.fastresume")

    fun save(file: File, bytes: ByteArray): Boolean {
        if (bytes.isEmpty() || bytes.size > MAX_BYTES) return false
        return try {
            file.parentFile?.mkdirs()
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeBytes(bytes)
            tmp.renameTo(file) || run { tmp.delete(); false }
        } catch (e: IOException) { false }
    }

    fun load(file: File): ByteArray? {
        return try {
            if (!file.isFile) return null
            val len = file.length()
            if (len <= 0L || len > MAX_BYTES) return null
            file.readBytes()
        } catch (e: IOException) { null }
    }

    fun delete(file: File) { runCatching { file.delete() } }
}
