package com.benzenelabs.hydra.host.io.manager

import com.benzenelabs.hydra.host.channel.ChannelId
import com.benzenelabs.hydra.host.data.config.ConfigStore
import com.benzenelabs.hydra.host.io.ConnectionHandle
import com.benzenelabs.hydra.host.io.ConnectionState
import com.benzenelabs.hydra.host.io.ConnectionType
import com.benzenelabs.hydra.host.io.IoFrame
import com.benzenelabs.hydra.host.io.connection.IoConnection
import com.benzenelabs.hydra.host.io.connection.http.HttpClient
import com.benzenelabs.hydra.host.io.connection.http.HttpClientImpl
import com.benzenelabs.hydra.host.io.connection.http.HttpRequest
import com.benzenelabs.hydra.host.io.connection.http.HttpResponse
import com.benzenelabs.hydra.host.io.connection.longpoll.LongPollConfig
import com.benzenelabs.hydra.host.io.connection.longpoll.LongPollConnectionImpl
import com.benzenelabs.hydra.host.io.connection.pipe.PipeConnection
import com.benzenelabs.hydra.host.io.connection.tcp.TcpConnectionImpl
import com.benzenelabs.hydra.host.io.connection.websocket.WebSocketConnectionImpl
import com.benzenelabs.hydra.host.io.policy.KeepAliveConfig
import com.benzenelabs.hydra.host.io.policy.ReconnectPolicy
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

/**
 * Default in-memory [IoConnectionManager] implementation.
 */
class IoConnectionManagerImpl(
    private val scope: CoroutineScope,
    private val configStore: ConfigStore,
    private val okHttpClient: OkHttpClient = OkHttpClient(),
    private val httpClient: HttpClient = HttpClientImpl(okHttpClient),
) : IoConnectionManager {

    private data class ManagedConnection(
        val channelId: ChannelId,
        val connection: IoConnection,
        val stateJob: Job
    )

    private val connections = ConcurrentHashMap<String, ManagedConnection>()
    private val _connectionStateUpdates = MutableSharedFlow<ConnectionHandle>(extraBufferCapacity = 256)

    override val connectionStateUpdates: Flow<ConnectionHandle> = _connectionStateUpdates

    override suspend fun openWebSocket(
        channelId: ChannelId,
        url: String,
        headers: Map<String, String>,
        reconnectPolicy: ReconnectPolicy,
        keepAlive: KeepAliveConfig
    ): ConnectionHandle {
        val handle = ConnectionHandle(
            id = ConnectionHandle.generateId(),
            type = ConnectionType.WEBSOCKET,
            channelId = channelId,
            remoteAddress = url,
            state = ConnectionState.IDLE
        )

        val connection = WebSocketConnectionImpl(
            handle = handle,
            httpHeaders = headers,
            reconnectPolicy = reconnectPolicy,
            keepAlive = keepAlive,
            okHttpClient = okHttpClient,
            scope = scope
        )

        register(connection)
        connection.open()
        return connection.handle
    }

    override suspend fun openTcp(
        channelId: ChannelId,
        address: String,
        reconnectPolicy: ReconnectPolicy
    ): ConnectionHandle {
        val handle = ConnectionHandle(
            id = ConnectionHandle.generateId(),
            type = ConnectionType.TCP,
            channelId = channelId,
            remoteAddress = address,
            state = ConnectionState.IDLE
        )

        val connection = TcpConnectionImpl(
            handle = handle,
            reconnectPolicy = reconnectPolicy,
            scope = scope
        )

        register(connection)
        connection.open()
        return connection.handle
    }

    override suspend fun startLongPoll(
        channelId: ChannelId,
        config: LongPollConfig
    ): ConnectionHandle {
        val handle = ConnectionHandle(
            id = ConnectionHandle.generateId(),
            type = ConnectionType.LONG_POLL,
            channelId = channelId,
            remoteAddress = config.url,
            state = ConnectionState.IDLE
        )

        val connection = LongPollConnectionImpl(
            handle = handle,
            config = config,
            channelId = channelId,
            configStore = configStore,
            httpClient = httpClient,
            scope = scope
        )

        register(connection)
        connection.open()
        return connection.handle
    }

    override suspend fun openPipe(channelId: ChannelId): ConnectionHandle {
        val handle = ConnectionHandle(
            id = ConnectionHandle.generateId(),
            type = ConnectionType.PIPE,
            channelId = channelId,
            remoteAddress = null,
            state = ConnectionState.IDLE
        )

        val connection = PipeConnection(handle = handle)
        register(connection)
        connection.open()
        return connection.handle
    }

    override suspend fun send(handle: ConnectionHandle, data: ByteArray) {
        val managed = connections[handle.id]
            ?: throw IllegalArgumentException("Connection not found: ${handle.id}")
        managed.connection.send(data)
    }

    override suspend fun close(handle: ConnectionHandle) {
        val managed = connections.remove(handle.id) ?: return
        managed.stateJob.cancel()
        managed.connection.close()
    }

    override suspend fun closeAll(channelId: ChannelId) {
        val targetIds = connections.entries
            .filter { (_, managed) -> managed.channelId == channelId }
            .map { (id, _) -> id }

        targetIds.forEach { id ->
            val managed = connections.remove(id) ?: return@forEach
            managed.stateJob.cancel()
            managed.connection.close()
        }
    }

    override fun stateOf(handle: ConnectionHandle): ConnectionState? =
        connections[handle.id]?.connection?.handle?.state

    override fun framesOf(handle: ConnectionHandle): Flow<IoFrame>? =
        connections[handle.id]?.connection?.frames

    override suspend fun http(request: HttpRequest): HttpResponse = httpClient.execute(request)

    override fun cancelHttp(cancellationKey: String) {
        httpClient.cancel(cancellationKey)
    }

    private fun register(connection: IoConnection) {
        val stateJob = scope.launch {
            connection.stateFlow.collect {
                _connectionStateUpdates.tryEmit(connection.handle)
            }
        }
        connections[connection.handle.id] = ManagedConnection(
            channelId = connection.handle.channelId,
            connection = connection,
            stateJob = stateJob
        )
    }
}
