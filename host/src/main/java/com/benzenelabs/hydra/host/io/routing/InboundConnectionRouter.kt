package com.benzenelabs.hydra.host.io.routing

import com.benzenelabs.hydra.host.channel.ChannelId

/**
 * Maps inbound remote addresses to channels.
 */
interface InboundConnectionRouter {
    /** Registers a remote address mapping. */
    fun register(remoteAddress: RemoteAddress, channelId: ChannelId)

    /** Removes one remote address mapping. */
    fun unregister(remoteAddress: RemoteAddress)

    /** Resolves the mapped channel for [remoteAddress], or null. */
    fun resolve(remoteAddress: RemoteAddress): ChannelId?

    /** Returns all active addresses mapped to [channelId]. */
    fun activeConnections(channelId: ChannelId): List<RemoteAddress>

    /** Removes all address mappings for [channelId]. */
    fun unregisterAll(channelId: ChannelId)
}
