package com.benzenelabs.hydra.host.io.dispatch

import com.benzenelabs.hydra.host.channel.ChannelId
import com.benzenelabs.hydra.host.channel.ChannelMessage
import com.benzenelabs.hydra.host.channel.MessageContent
import com.benzenelabs.hydra.host.channel.MessageDirection
import com.benzenelabs.hydra.host.data.session.SessionId
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InboundMessageQueueTest {

    private val queue = InboundMessageQueue(capacityPerSession = 2)
    private val sid = SessionId(UUID.randomUUID().toString())

    @Test
    fun `fifo order`() {
        val m1 = msg("1")
        val m2 = msg("2")

        queue.enqueue(sid, m1)
        queue.enqueue(sid, m2)

        val drained = queue.drain(sid)
        assertEquals(2, drained.size)
        assertEquals(m1, drained[0])
        assertEquals(m2, drained[1])

        assertEquals(0, queue.size(sid))
    }

    @Test
    fun `capacity overflow drops oldest`() {
        val m1 = msg("1")
        val m2 = msg("2")
        val m3 = msg("3")

        assertTrue(queue.enqueue(sid, m1))
        assertTrue(queue.enqueue(sid, m2))
        // Capacity is 2. Enqueuing 3rd should drop m1 and return false
        assertFalse(queue.enqueue(sid, m3))

        assertEquals(2, queue.size(sid))
        val drained = queue.drain(sid)
        assertEquals(m2, drained[0])
        assertEquals(m3, drained[1])
    }

    @Test
    fun `clear removes messages`() {
        queue.enqueue(sid, msg("1"))
        queue.clear(sid)
        assertEquals(0, queue.size(sid))
        assertTrue(queue.drain(sid).isEmpty())
    }

    private fun msg(id: String) =
            ChannelMessage(
                id = id,
                sessionId = sid,
                content = MessageContent.Text("{}"),
                channelId = ChannelId("dev.namer"),
                direction = MessageDirection.INBOUND,
                replyToId = null,
                metadata = null,
                timestamp = 10L
            )
}
