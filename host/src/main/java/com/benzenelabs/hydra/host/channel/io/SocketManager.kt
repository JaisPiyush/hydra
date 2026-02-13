package com.benzenelabs.hydra.host.channel.io

import com.benzenelabs.hydra.host.channel.ChannelId
import com.benzenelabs.hydra.host.channel.ConnectionEvent
import kotlinx.coroutines.flow.Flow

/**
 * Manages raw socket connections for channel extensions.
 */
interface SocketManager {

    /**
     * Opens a new socket connection and returns a [SocketHandle].
     *
     * @throws SocketConnectException if the connection cannot be established.
     */
    suspend fun connect(
        channelId: ChannelId,
        url: String,
        protocol: SocketProtocol,
        headers: Map<String, String> = emptyMap()
    ): SocketHandle

    /**
     * Sends binary data over an open socket.
     *
     * @throws SocketNotFoundException if [handle] is not a known open socket.
     * @throws SocketSendException if the write fails.
     */
    suspend fun send(handle: SocketHandle, data: ByteArray)

    /**
     * Sends a text frame (WebSocket only).
     *
     * @throws SocketNotFoundException if [handle] is unknown.
     * @throws UnsupportedOperationException if [handle.protocol] is not WEBSOCKET.
     */
    suspend fun sendText(handle: SocketHandle, text: String)

    /**
     * Closes an open socket gracefully.
     * Idempotent - does not throw if already closed.
     */
    suspend fun close(handle: SocketHandle, reason: String = "Normal closure")

    /**
     * Closes all sockets owned by [channelId].
     * Called during channel unregistration.
     */
    suspend fun closeAll(channelId: ChannelId)

    /**
     * Returns a [Flow] of [ConnectionEvent]s for all sockets owned by [channelId].
     */
    fun connectionEvents(channelId: ChannelId): Flow<ConnectionEvent>
}

class SocketConnectException(url: String, cause: Throwable? = null) :
    RuntimeException("Failed to connect to $url", cause)

class SocketNotFoundException(socketId: String) :
    IllegalArgumentException("Socket not found: $socketId")

class SocketSendException(socketId: String, cause: Throwable? = null) :
    RuntimeException("Failed to send on socket $socketId", cause)
