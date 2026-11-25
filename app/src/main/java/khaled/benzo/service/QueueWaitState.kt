package khaled.benzo.service

import khaled.benzo.core.queue.BlockReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Why each queued download is waiting right now (download id to reason). Updated by the queue loop; read by the list. In memory only. */
object QueueWaitState {
    private val state = MutableStateFlow<Map<Long, BlockReason>>(emptyMap())
    val reasons: StateFlow<Map<Long, BlockReason>> get() = state
    fun publish(map: Map<Long, BlockReason>) { if (state.value != map) state.value = map }
}
