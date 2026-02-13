package com.benzenelabs.hydra.host.channel

import kotlinx.coroutines.flow.SharedFlow

/**
 * Host-to-JS bridge contract for a channel extension.
 */
interface ChannelBridge {

    val channelId: ChannelId

    /**
     * Delivers raw bytes from the I/O layer to the JS extension.
     */
    suspend fun deliverToExtension(socketId: String, data: ByteArray)

    /**
     * Notifies the extension that a socket connection event occurred.
     */
    suspend fun notifyConnectionEvent(event: ConnectionEvent)

    /**
     * Called by the JS extension to deliver a decoded inbound message.
     */
    suspend fun emitInboundMessage(message: ChannelMessage)

    /**
     * Called by the JS extension to update the host-side [ChannelState] machine.
     */
    suspend fun reportStateChange(newState: ChannelState, errorMessage: String? = null)

    /**
     * Called by the JS extension when the channel requires user authentication.
     */
    suspend fun reportAuthRequest(request: AuthRequest)

    /**
     * Called by the JS extension to confirm or deny the result of a send operation.
     */
    suspend fun notifyOutboundResult(
        messageId: String,
        success: Boolean,
        errorReason: String? = null
    )

    /**
     * Asks the extension to send [message] to the external platform.
     *
     * @throws ChannelNotConnectedException if the channel is not in [ChannelState.CONNECTED].
     */
    suspend fun sendMessage(message: ChannelMessage)

    /** A [SharedFlow] of [ChannelEvent] emitted by this bridge. */
    val events: SharedFlow<ChannelEvent>

    /**
     * Releases resources held by this bridge.
     *
     * Implementations may close underlying channels/connections.
     * Idempotent by contract.
     */
    suspend fun close() = Unit
}

/** Lifecycle event from the socket/transport layer for a specific socket. */
sealed class ConnectionEvent {
    data class Opened(val socketId: String) : ConnectionEvent()
    data class Closed(val socketId: String, val code: Int, val reason: String) : ConnectionEvent()
    data class Error(val socketId: String, val cause: Throwable) : ConnectionEvent()
}

class ChannelNotConnectedException(channelId: ChannelId) :
    IllegalStateException("Channel ${channelId.value} is not in CONNECTED state")
