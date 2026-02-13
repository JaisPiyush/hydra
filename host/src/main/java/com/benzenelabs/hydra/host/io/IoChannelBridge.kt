package com.benzenelabs.hydra.host.io

import com.benzenelabs.hydra.host.channel.AuthRequest
import com.benzenelabs.hydra.host.channel.ChannelBridge
import com.benzenelabs.hydra.host.channel.ChannelEvent
import com.benzenelabs.hydra.host.channel.ChannelId
import com.benzenelabs.hydra.host.channel.ChannelMessage
import com.benzenelabs.hydra.host.channel.ChannelNotConnectedException
import com.benzenelabs.hydra.host.channel.ChannelState
import com.benzenelabs.hydra.host.channel.ConnectionEvent
import com.benzenelabs.hydra.host.io.connection.longpoll.LongPollConfig
import com.benzenelabs.hydra.host.io.manager.IoConnectionManager
import com.benzenelabs.hydra.host.io.policy.KeepAliveConfig
import com.benzenelabs.hydra.host.io.policy.ReconnectPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * [ChannelBridge] implementation backed by [IoConnectionManager].
 *
 * This bridge is the integration point between `host/channel` and `host/io`.
 * It tracks connection handles for a channel, forwards raw inbound frames into
 * the extension callback, and surfaces connection lifecycle updates.
 */
class IoChannelBridge(
    override val channelId: ChannelId,
    private val ioConnectionManager: IoConnectionManager,
    private val scope: CoroutineScope,
    private val extensionReceiver: suspend (connectionId: String, data: ByteArray) -> Unit,
    private val extensionConnectionEventReceiver: suspend (ConnectionEvent) -> Unit,
    private val extensionSender: suspend (ChannelMessage) -> Unit,
    private val stateUpdateReporter: suspend (ChannelState, String?) -> Unit = { _, _ -> }
) : ChannelBridge {

    private val mutex = Mutex()
    private var state: ChannelState = ChannelState.REGISTERED
    private val pendingOutbound = linkedMapOf<String, ChannelMessage>()
    private val activeHandles = linkedMapOf<String, ConnectionHandle>()
    private val frameJobs = linkedMapOf<String, Job>()

    private val eventFlow = MutableSharedFlow<ChannelEvent>(extraBufferCapacity = 128)

    private val stateEventsJob: Job = scope.launch {
        ioConnectionManager.connectionStateUpdates.collect { handle ->
            if (handle.channelId != channelId) return@collect
            mutex.withLock {
                if (handle.state == ConnectionState.CLOSED || handle.state == ConnectionState.FAILED) {
                    activeHandles.remove(handle.id)
                    frameJobs.remove(handle.id)?.cancel()
                } else {
                    activeHandles[handle.id] = handle
                }
            }
            when (handle.state) {
                ConnectionState.CONNECTED -> {
                    extensionConnectionEventReceiver(ConnectionEvent.Opened(handle.id))
                }

                ConnectionState.CLOSED -> {
                    extensionConnectionEventReceiver(
                        ConnectionEvent.Closed(
                            socketId = handle.id,
                            code = 1000,
                            reason = "Closed"
                        )
                    )
                }

                ConnectionState.FAILED -> {
                    extensionConnectionEventReceiver(
                        ConnectionEvent.Error(
                            socketId = handle.id,
                            cause = IllegalStateException("Connection failed: ${handle.id}")
                        )
                    )
                }

                else -> Unit
            }
        }
    }

    override val events: SharedFlow<ChannelEvent> = eventFlow.asSharedFlow()

    /** Opens a managed outbound WebSocket for this channel. */
    suspend fun openWebSocket(
        url: String,
        headers: Map<String, String> = emptyMap(),
        reconnectPolicy: ReconnectPolicy = ReconnectPolicy.DEFAULT,
        keepAlive: KeepAliveConfig = KeepAliveConfig.DEFAULT
    ): ConnectionHandle {
        val handle = ioConnectionManager.openWebSocket(
            channelId = channelId,
            url = url,
            headers = headers,
            reconnectPolicy = reconnectPolicy,
            keepAlive = keepAlive
        )
        registerHandle(handle)
        return handle
    }

    /** Opens a managed outbound TCP connection for this channel. */
    suspend fun openTcp(
        address: String,
        reconnectPolicy: ReconnectPolicy = ReconnectPolicy.DEFAULT
    ): ConnectionHandle {
        val handle = ioConnectionManager.openTcp(
            channelId = channelId,
            address = address,
            reconnectPolicy = reconnectPolicy
        )
        registerHandle(handle)
        return handle
    }

    /** Starts a managed long-poll connection for this channel. */
    suspend fun startLongPoll(config: LongPollConfig): ConnectionHandle {
        val handle = ioConnectionManager.startLongPoll(channelId = channelId, config = config)
        registerHandle(handle)
        return handle
    }

    /** Opens an in-process pipe connection for this channel. */
    suspend fun openPipe(): ConnectionHandle {
        val handle = ioConnectionManager.openPipe(channelId = channelId)
        registerHandle(handle)
        return handle
    }

    /** Sends binary data on a specific managed connection id. */
    suspend fun send(connectionId: String, data: ByteArray) {
        val handle = mutex.withLock { activeHandles[connectionId] }
            ?: throw IllegalArgumentException("Connection not found: $connectionId")
        ioConnectionManager.send(handle, data)
    }

    /** Sends UTF-8 text on a specific managed connection id. */
    suspend fun sendText(connectionId: String, text: String) {
        send(connectionId, text.toByteArray(Charsets.UTF_8))
    }

    /** Closes one managed connection. */
    suspend fun closeConnection(connectionId: String) {
        val (handle, frameJob) = mutex.withLock {
            activeHandles.remove(connectionId) to frameJobs.remove(connectionId)
        }
        if (handle == null) return
        frameJob?.cancel()
        ioConnectionManager.close(handle)
    }

    /** Closes all managed connections for this channel. */
    suspend fun closeAllConnections() {
        ioConnectionManager.closeAll(channelId)
        mutex.withLock {
            activeHandles.clear()
            frameJobs.values.forEach { it.cancel() }
            frameJobs.clear()
        }
    }

    override suspend fun deliverToExtension(socketId: String, data: ByteArray) {
        extensionReceiver(socketId, data)
    }

    override suspend fun notifyConnectionEvent(event: ConnectionEvent) {
        extensionConnectionEventReceiver(event)
    }

    override suspend fun emitInboundMessage(message: ChannelMessage) {
        eventFlow.emit(ChannelEvent.MessageReceived(message))
    }

    override suspend fun reportStateChange(newState: ChannelState, errorMessage: String?) {
        val previous = mutex.withLock {
            if (!state.canTransitionTo(newState)) {
                throw IllegalArgumentException("Invalid channel state transition: $state -> $newState")
            }
            val old = state
            state = newState
            old
        }
        stateUpdateReporter(newState, errorMessage)
        eventFlow.emit(
            ChannelEvent.StateChanged(
                channelId = channelId,
                previousState = previous,
                newState = newState,
                errorMessage = errorMessage
            )
        )
    }

    override suspend fun reportAuthRequest(request: AuthRequest) {
        eventFlow.emit(ChannelEvent.AuthRequired(channelId, request))
    }

    override suspend fun notifyOutboundResult(
        messageId: String,
        success: Boolean,
        errorReason: String?
    ) {
        require(messageId.isNotBlank()) { "messageId must not be blank" }
        val message = mutex.withLock {
            pendingOutbound.remove(messageId)
        } ?: return

        if (success) {
            eventFlow.emit(ChannelEvent.MessageSent(message))
        } else {
            eventFlow.emit(
                ChannelEvent.MessageSendFailed(
                    sessionId = message.sessionId,
                    channelId = message.channelId,
                    reason = errorReason ?: "Unknown outbound failure",
                    cause = null
                )
            )
        }
    }

    override suspend fun sendMessage(message: ChannelMessage) {
        val isConnected = mutex.withLock { state == ChannelState.CONNECTED }
        if (!isConnected) {
            throw ChannelNotConnectedException(channelId)
        }
        mutex.withLock {
            pendingOutbound[message.id] = message
        }
        extensionSender(message)
    }

    override suspend fun close() {
        stateEventsJob.cancel()
        closeAllConnections()
        mutex.withLock {
            pendingOutbound.clear()
            state = ChannelState.UNREGISTERED
        }
    }

    private suspend fun registerHandle(handle: ConnectionHandle) {
        val frameJob = scope.launch {
            ioConnectionManager.framesOf(handle)?.collect { frame ->
                extensionReceiver(frame.connectionId, frame.payload)
            }
        }
        mutex.withLock {
            activeHandles[handle.id] = handle
            frameJobs[handle.id] = frameJob
        }
    }
}
