package com.benzenelabs.hydra.host.io.dispatch

import com.benzenelabs.hydra.host.channel.ChannelId
import com.benzenelabs.hydra.host.channel.ChannelMessage
import com.benzenelabs.hydra.host.data.session.SessionStore

/**
 * Default [IoMessageDispatcher] implementation.
 */
class IoMessageDispatcherImpl(
    private val inboundQueue: InboundMessageQueue,
    private val runtimeWaker: RuntimeWaker,
    private val sessionStore: SessionStore
) : IoMessageDispatcher {

    override suspend fun dispatch(channelId: ChannelId, message: ChannelMessage) {
        val resolvedSession = sessionStore.findByRemote(channelId, message.sessionId.value)
        val sessionId = resolvedSession?.id ?: message.sessionId
        inboundQueue.enqueue(sessionId, message)
        runtimeWaker.wake(sessionId, channelId)
    }
}
