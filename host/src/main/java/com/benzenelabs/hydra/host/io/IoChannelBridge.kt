package com.benzenelabs.hydra.host.io

import com.benzenelabs.hydra.host.channel.AuthRequest
import com.benzenelabs.hydra.host.channel.ChannelBridge
import com.benzenelabs.hydra.host.channel.ChannelEvent
import com.benzenelabs.hydra.host.channel.ChannelId
import com.benzenelabs.hydra.host.channel.ChannelMessage
import com.benzenelabs.hydra.host.channel.ChannelNotConnectedException
import com.benzenelabs.hydra.host.channel.ChannelState
import com.benzenelabs.hydra.host.channel.ConnectionEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * [ChannelBridge] implementation for socket-based channels.
 */
class IoChannelBridge(
    override val channelId: ChannelId,
    private val extensionReceiver: suspend (socketId: String, data: ByteArray) -> Unit,
    private val extensionConnectionEventReceiver: suspend (ConnectionEvent) -> Unit,
    private val extensionSender: suspend (ChannelMessage) -> Unit,
    private val stateUpdateReporter: suspend (ChannelState, String?) -> Unit = { _, _ -> }
) : ChannelBridge {

    private val mutex = Mutex()
    private var state: ChannelState = ChannelState.REGISTERED
    private val pendingOutbound = linkedMapOf<String, ChannelMessage>()
    private val eventFlow = MutableSharedFlow<ChannelEvent>(extraBufferCapacity = 128)

    override val events: SharedFlow<ChannelEvent> = eventFlow.asSharedFlow()

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

    override suspend fun notifyOutboundResult(messageId: String, success: Boolean, errorReason: String?) {
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

    /**
     * Clears pending outbound state so the bridge can be released safely.
     */
    suspend fun close() {
        mutex.withLock {
            pendingOutbound.clear()
            state = ChannelState.UNREGISTERED
        }
    }
}
