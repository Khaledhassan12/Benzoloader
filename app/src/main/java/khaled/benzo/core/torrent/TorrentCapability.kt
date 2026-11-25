package khaled.benzo.core.torrent

/**
 * Decides once whether the native torrent library can be used on this device and keeps the reason when it cannot.
 * Only the three errors a failed native load produces are caught: UnsatisfiedLinkError (no library for this CPU or
 * a missing symbol), ExceptionInInitializerError (the library's static initializer threw) and NoClassDefFoundError
 * (a class of a failed initializer used again). Everything else, including programming errors, is not caught here.
 * HTTP downloads and the list never depend on this class.
 */
class TorrentCapability<T : Any>(private val create: () -> T) {
    private var engine: T? = null
    private var failure: String? = null
    private var tried = false

    /** The engine, or null when the native library cannot load. The attempt is made once. */
    @Synchronized
    fun engineOrNull(): T? {
        if (!tried) {
            tried = true
            try {
                engine = create()
            } catch (e: UnsatisfiedLinkError) {
                failure = describe(e)
            } catch (e: ExceptionInInitializerError) {
                failure = describe(e)
            } catch (e: NoClassDefFoundError) {
                failure = describe(e)
            }
        }
        return engine
    }

    /** Why torrents are unavailable, or null when they work or have not been tried yet. */
    @Synchronized
    fun diagnostic(): String? = failure

    private fun describe(e: Throwable): String = "${e.javaClass.simpleName}: ${e.message ?: "no message"}"
}
