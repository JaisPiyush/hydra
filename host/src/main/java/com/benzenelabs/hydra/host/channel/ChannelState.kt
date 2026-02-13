package com.benzenelabs.hydra.host.channel

/**
 * The connection/lifecycle state of a registered channel.
 */
enum class ChannelState {

    /** Extension is not yet loaded or has been unregistered. */
    UNREGISTERED,

    /** Extension is loaded and registered; no connection attempt yet. */
    REGISTERED,

    /** Actively attempting to establish the platform connection. */
    CONNECTING,

    /**
     * Connection established; performing platform-specific authentication.
     */
    AUTHENTICATING,

    /** Fully connected and authenticated. Ready to exchange messages. */
    CONNECTED,

    /**
     * Connection lost; extension is performing automatic reconnection with backoff.
     */
    RECONNECTING,

    /**
     * Connection terminated. Not reconnecting automatically.
     */
    DISCONNECTED,

    /**
     * Terminal error requiring user intervention.
     */
    ERROR;

    fun canTransitionTo(next: ChannelState): Boolean = when (this) {
        UNREGISTERED -> next == REGISTERED
        REGISTERED -> next == CONNECTING || next == UNREGISTERED
        CONNECTING -> next == AUTHENTICATING || next == DISCONNECTED || next == ERROR
        AUTHENTICATING -> next == CONNECTED || next == DISCONNECTED || next == ERROR
        CONNECTED -> next == RECONNECTING || next == DISCONNECTED || next == ERROR
        RECONNECTING -> next == CONNECTED || next == DISCONNECTED || next == ERROR
        DISCONNECTED -> next == CONNECTING || next == UNREGISTERED
        ERROR -> next == UNREGISTERED
    }
}
