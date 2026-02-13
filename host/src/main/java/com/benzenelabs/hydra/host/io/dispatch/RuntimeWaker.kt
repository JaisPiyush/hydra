package com.benzenelabs.hydra.host.io.dispatch

import com.benzenelabs.hydra.host.channel.ChannelId
import com.benzenelabs.hydra.host.data.session.SessionId

/**
 * Notifies runtime that a message is queued for a session.
 */
interface RuntimeWaker {
    /** Non-blocking and idempotent wake signal. */
    fun wake(sessionId: SessionId, channelId: ChannelId)
}
