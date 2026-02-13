package com.benzenelabs.hydra.host.io

import com.benzenelabs.hydra.host.channel.ChannelId
import java.util.UUID

/**
 * Opaque reference to a managed transport connection.
 */
data class ConnectionHandle(
    val id: String,
    val type: ConnectionType,
    val channelId: ChannelId,
    val remoteAddress: String?,
    val state: ConnectionState
) {
    init {
        require(id.isNotBlank()) { "ConnectionHandle.id must not be blank" }
        UUID.fromString(id)
        if (type != ConnectionType.PIPE && type != ConnectionType.HTTP) {
            require(!remoteAddress.isNullOrBlank()) {
                "ConnectionHandle.remoteAddress is required for type $type"
            }
        }
    }

    /** Returns a copied handle with updated [newState]. */
    fun withState(newState: ConnectionState): ConnectionHandle {
        require(state.canTransitionTo(newState)) {
            "Invalid connection state transition: $state -> $newState for $id"
        }
        return copy(state = newState)
    }

    companion object {
        /** Generates a random UUID connection id. */
        fun generateId(): String = UUID.randomUUID().toString()
    }
}
