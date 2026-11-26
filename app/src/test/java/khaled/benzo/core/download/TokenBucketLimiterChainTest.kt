package khaled.benzo.core.download

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

/** WRITTEN, NOT RUN. A per-download limiter chained to the global one cannot exceed the global cap. */
class TokenBucketLimiterChainTest {
    @Test fun childCannotBypassParent() = runBlocking {
        val global = TokenBucketLimiter(10_000L)
        global.setLimit(10_000L)
        val child = TokenBucketLimiter(10_000_000L, parent = global)
        val start = System.currentTimeMillis()
        repeat(4) { child.acquire(5_000) }   // 20 000 bytes through a 10 000 B/s global cap
        val ms = System.currentTimeMillis() - start
        assertTrue("took $ms ms", ms >= 800)
    }
}
