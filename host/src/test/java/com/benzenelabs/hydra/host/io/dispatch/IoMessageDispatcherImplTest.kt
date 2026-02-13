package com.benzenelabs.hydra.host.io.dispatch

import com.benzenelabs.hydra.contributions.api.ContributionId
import com.benzenelabs.hydra.host.channel.ChannelId
import com.benzenelabs.hydra.host.channel.ChannelMessage
import com.benzenelabs.hydra.host.channel.MessageContent
import com.benzenelabs.hydra.host.channel.MessageDirection
import com.benzenelabs.hydra.host.data.session.Session
import com.benzenelabs.hydra.host.data.session.SessionId
import com.benzenelabs.hydra.host.data.session.SessionState
import com.benzenelabs.hydra.host.io.fakes.FakeRuntimeWaker
import com.benzenelabs.hydra.host.io.fakes.FakeSessionStore
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IoMessageDispatcherImplTest {

    @Test
    fun `dispatch enqueues and wakes`() = runTest {
        val queue = InboundMessageQueue()
        val waker = FakeRuntimeWaker()
        val sessions = FakeSessionStore()
        val dispatcher = IoMessageDispatcherImpl(queue, waker, sessions)

        val chId = ChannelId("ch.1")
        val sid = SessionId(UUID.randomUUID().toString())

        // Ensure session exists
        sessions.create(
                Session(
                        id = sid,
                        channelId = ContributionId(chId.value),
                        remoteId = "u1",
                        createdAt = 1000,
                        updatedAt = 1000,
                        displayName = "Untitled",
                        authRef = null,
                        metadata = null,
                        lastMessageAt = null,
                        state = SessionState.ACTIVE,

                )
        )

        val message = ChannelMessage(
            id = "m1", sessionId = sid,
            channelId = chId,
            direction = MessageDirection.INBOUND,
            content = MessageContent.Text("{}"),
            replyToId = null,
            timestamp = 10L,
            metadata = null
        )

        dispatcher.dispatch(chId, message)

        // Check queue
        assertEquals(1, queue.size(sid))

        // Check waker
        assertEquals(1, waker.wakeCalls.size)
        assertEquals(sid, waker.wakeCalls[0].first)
        assertEquals(chId, waker.wakeCalls[0].second)
    }
}
