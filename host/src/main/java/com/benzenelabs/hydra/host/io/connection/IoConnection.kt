package com.benzenelabs.hydra.host.io.connection

import com.benzenelabs.hydra.host.io.ConnectionHandle
import com.benzenelabs.hydra.host.io.ConnectionState
import com.benzenelabs.hydra.host.io.IoFrame
import kotlinx.coroutines.flow.Flow

/**
 * Common abstraction for persistent I/O transports.
 */
interface IoConnection {

    val handle: ConnectionHandle

    /**
     * Opens this connection and suspends until connected or failure.
     *
     * @throws IoConnectException on connection failure.
     */
    suspend fun open()

    /**
     * Sends raw bytes over this connection.
     *
     * @throws IoSendException on write failure.
     * @throws IllegalStateException if not connected.
     */
    suspend fun send(data: ByteArray)

    /** Sends UTF-8 text via [send]. */
    suspend fun sendText(text: String) = send(text.toByteArray(Charsets.UTF_8))

    /** Closes this connection gracefully. Idempotent. */
    suspend fun close()

    /** Emits inbound frames in arrival order. */
    val frames: Flow<IoFrame>

    /** Emits state transitions and replays current state on collection. */
    val stateFlow: Flow<ConnectionState>
}

/** Connection establishment failure. */
class IoConnectException(url: String, cause: Throwable? = null) :
    RuntimeException("Failed to connect: $url", cause)

/** Connection send failure. */
class IoSendException(connectionId: String, cause: Throwable? = null) :
    RuntimeException("Send failed on connection: $connectionId", cause)
