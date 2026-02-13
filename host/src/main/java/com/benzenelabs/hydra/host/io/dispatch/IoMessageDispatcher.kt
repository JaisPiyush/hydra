package com.benzenelabs.hydra.host.io.dispatch

import com.benzenelabs.hydra.host.channel.ChannelId
import com.benzenelabs.hydra.host.channel.ChannelMessage

/**
 * Routes decoded channel messages into runtime intake queues.
 */
interface IoMessageDispatcher {
    /** Enqueues [message] and wakes runtime for [channelId]. */
    suspend fun dispatch(channelId: ChannelId, message: ChannelMessage)
}
