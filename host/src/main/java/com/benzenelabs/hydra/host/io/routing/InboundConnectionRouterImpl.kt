package com.benzenelabs.hydra.host.io.routing

import com.benzenelabs.hydra.host.channel.ChannelId
import java.util.concurrent.ConcurrentHashMap

/**
 * Lock-free [InboundConnectionRouter] based on a concurrent map.
 */
class InboundConnectionRouterImpl : InboundConnectionRouter {

    private val mappings = ConcurrentHashMap<RemoteAddress, ChannelId>()

    override fun register(remoteAddress: RemoteAddress, channelId: ChannelId) {
        mappings[remoteAddress] = channelId
    }

    override fun unregister(remoteAddress: RemoteAddress) {
        mappings.remove(remoteAddress)
    }

    override fun resolve(remoteAddress: RemoteAddress): ChannelId? = mappings[remoteAddress]

    override fun activeConnections(channelId: ChannelId): List<RemoteAddress> =
        mappings.entries
            .asSequence()
            .filter { (_, mappedChannelId) -> mappedChannelId == channelId }
            .map { (remoteAddress, _) -> remoteAddress }
            .toList()

    override fun unregisterAll(channelId: ChannelId) {
        mappings.entries.removeIf { (_, mappedChannelId) -> mappedChannelId == channelId }
    }
}
