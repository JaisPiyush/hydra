package com.benzenelabs.hydra.host.io.manager

import com.benzenelabs.hydra.host.channel.ChannelId
import com.benzenelabs.hydra.host.io.ConnectionHandle
import com.benzenelabs.hydra.host.io.ConnectionState
import com.benzenelabs.hydra.host.io.IoFrame
import com.benzenelabs.hydra.host.io.connection.http.HttpRequest
import com.benzenelabs.hydra.host.io.connection.http.HttpResponse
import com.benzenelabs.hydra.host.io.connection.longpoll.LongPollConfig
import com.benzenelabs.hydra.host.io.policy.KeepAliveConfig
import com.benzenelabs.hydra.host.io.policy.ReconnectPolicy
import kotlinx.coroutines.flow.Flow

/**
 * Top-level facade for all host I/O connection operations.
 */
interface IoConnectionManager {

    /** Opens and registers a persistent outbound WebSocket connection. */
    suspend fun openWebSocket(
        channelId: ChannelId,
        url: String,
        headers: Map<String, String> = emptyMap(),
        reconnectPolicy: ReconnectPolicy = ReconnectPolicy.DEFAULT,
        keepAlive: KeepAliveConfig = KeepAliveConfig.DEFAULT
    ): ConnectionHandle

    /** Opens and registers a persistent outbound TCP connection. */
    suspend fun openTcp(
        channelId: ChannelId,
        address: String,
        reconnectPolicy: ReconnectPolicy = ReconnectPolicy.DEFAULT
    ): ConnectionHandle

    /** Starts and registers a host-managed long-poll loop. */
    suspend fun startLongPoll(
        channelId: ChannelId,
        config: LongPollConfig
    ): ConnectionHandle

    /** Opens and registers an in-process pipe connection. */
    suspend fun openPipe(channelId: ChannelId): ConnectionHandle

    /** Sends raw bytes using [handle]. */
    suspend fun send(handle: ConnectionHandle, data: ByteArray)

    /** Closes one connection. */
    suspend fun close(handle: ConnectionHandle)

    /** Closes all connections owned by [channelId]. */
    suspend fun closeAll(channelId: ChannelId)

    /** Returns current state for [handle], or null if unknown. */
    fun stateOf(handle: ConnectionHandle): ConnectionState?

    /** Returns inbound frame flow for [handle], or null if unknown. */
    fun framesOf(handle: ConnectionHandle): Flow<IoFrame>?

    /** Emits updated handles whenever any connection state changes. */
    val connectionStateUpdates: Flow<ConnectionHandle>

    /** Executes one outbound HTTP request. */
    suspend fun http(request: HttpRequest): HttpResponse

    /** Cancels in-flight HTTP requests by key. */
    fun cancelHttp(cancellationKey: String)
}
