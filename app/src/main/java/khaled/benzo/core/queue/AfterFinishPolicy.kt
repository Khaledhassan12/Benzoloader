package khaled.benzo.core.queue

/** What the service does when the last piece of work ends. Stored as its [code]. */
enum class AfterFinish(val code: Int) {
    NOTHING(0), EXIT_SERVICE(1);

    companion object {
        fun fromCode(code: Int): AfterFinish = values().firstOrNull { it.code == code } ?: NOTHING
    }
}

object AfterFinishPolicy {
    /**
     * True when the service should stop now: the user chose exit, a download just completed, and nothing is running,
     * waiting to start, or being seeded. A failure never triggers it, so the user sees what went wrong.
     */
    fun shouldExit(mode: AfterFinish, justCompleted: Boolean, running: Int, waiting: Int, seeding: Int): Boolean =
        mode == AfterFinish.EXIT_SERVICE && justCompleted && running == 0 && waiting == 0 && seeding == 0
}
