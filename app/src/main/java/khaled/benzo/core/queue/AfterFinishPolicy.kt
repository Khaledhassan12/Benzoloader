package khaled.benzo.core.queue

/** What the service does when the last piece of work ends. Stored as its [code]. */
enum class AfterFinish(val code: Int) {
    NOTHING(0), EXIT_SERVICE(1);

    companion object {
        fun fromCode(code: Int): AfterFinish = values().firstOrNull { it.code == code } ?: NOTHING
    }
}

object AfterFinishPolicy {
    /** Mode for one finished download: its own choice (1 = nothing, 2 = exit) wins, 0 or anything else follows the general setting. */
    fun effective(itemCode: Int, general: AfterFinish): AfterFinish = when (itemCode) {
        1 -> AfterFinish.NOTHING
        2 -> AfterFinish.EXIT_SERVICE
        else -> general
    }

    /**
     * True when the service should stop now: the user chose exit, a download just completed, and nothing is running,
     * waiting to start, or being seeded. A failure never triggers it, so the user sees what went wrong.
     */
    fun shouldExit(mode: AfterFinish, justCompleted: Boolean, running: Int, waiting: Int, seeding: Int): Boolean =
        mode == AfterFinish.EXIT_SERVICE && allFinished(justCompleted, running, waiting, seeding)

    /**
     * True when a download just completed and nothing else is running, waiting to start or being seeded. Used for the
     * "total finish" vibration. INFERRED meaning: the reference lists a "total finish" vibration beside intercept, success
     * and error (OBSERVED rows, value 500 ms); that it means "queue now empty" was not seen.
     */
    fun allFinished(justCompleted: Boolean, running: Int, waiting: Int, seeding: Int): Boolean =
        justCompleted && running == 0 && waiting == 0 && seeding == 0
}
