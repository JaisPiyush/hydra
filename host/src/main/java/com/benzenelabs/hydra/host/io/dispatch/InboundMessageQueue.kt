package com.benzenelabs.hydra.host.io.dispatch

import com.benzenelabs.hydra.host.channel.ChannelMessage
import com.benzenelabs.hydra.host.data.session.SessionId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingDeque

/**
 * Per-session bounded FIFO queue for inbound [ChannelMessage]s.
 */
class InboundMessageQueue(val capacityPerSession: Int = 100) {
    init {
        require(capacityPerSession > 0) { "capacityPerSession must be > 0" }
    }

    private val queues = ConcurrentHashMap<String, LinkedBlockingDeque<ChannelMessage>>()

    /**
     * Enqueues [message] for [sessionId].
     * Returns true when no eviction occurs; false when oldest is dropped.
     */
    fun enqueue(sessionId: SessionId, message: ChannelMessage): Boolean {
        val queue = queues.getOrPut(sessionId.value) { LinkedBlockingDeque(capacityPerSession) }
        return if (queue.size >= capacityPerSession) {
            queue.pollFirst()
            queue.offerLast(message)
            false
        } else {
            queue.offerLast(message)
            true
        }
    }

    /** Drains and removes all queued messages for [sessionId] in FIFO order. */
    fun drain(sessionId: SessionId): List<ChannelMessage> {
        val queue = queues.remove(sessionId.value) ?: return emptyList()
        return buildList {
            while (queue.isNotEmpty()) {
                add(queue.pollFirst()!!)
            }
        }
    }

    /** Returns queue size for [sessionId]. */
    fun size(sessionId: SessionId): Int = queues[sessionId.value]?.size ?: 0

    /** Removes queue for [sessionId]. */
    fun clear(sessionId: SessionId) {
        queues.remove(sessionId.value)
    }
}
