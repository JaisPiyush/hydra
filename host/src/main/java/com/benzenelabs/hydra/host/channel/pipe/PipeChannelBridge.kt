package com.benzenelabs.hydra.host.channel.pipe

import com.benzenelabs.hydra.host.channel.AuthRequest
import com.benzenelabs.hydra.host.channel.ChannelBridge
import com.benzenelabs.hydra.host.channel.ChannelEvent
import com.benzenelabs.hydra.host.channel.ChannelId
import com.benzenelabs.hydra.host.channel.ChannelMessage
import com.benzenelabs.hydra.host.channel.ChannelNotConnectedException
import com.benzenelabs.hydra.host.channel.ChannelState
import com.benzenelabs.hydra.host.channel.ConnectionEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * [ChannelBridge] for in-process channels backed by coroutine [Channel]s.
 */
class PipeChannelBridge(
    override val channelId: ChannelId,
    private val inboundToExtension: Channel<ChannelMessage>,
    private val outboundFromExtension: Channel<ChannelMessage>
) : ChannelBridge {

    private val mutex = Mutex()
    private var state: ChannelState = ChannelState.REGISTERED
    private val eventFlow = MutableSharedFlow<ChannelEvent>(extraBufferCapacity = 128)

    override val events: SharedFlow<ChannelEvent> = eventFlow.asSharedFlow()

    override suspend fun deliverToExtension(socketId: String, data: ByteArray) {
        // No-op: pipe bridge does not use raw socket data.
    }

    override suspend fun notifyConnectionEvent(event: ConnectionEvent) {
        // No-op: pipe bridge has no socket lifecycle.
    }

    override suspend fun emitInboundMessage(message: ChannelMessage) {
        outboundFromExtension.send(message)
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
        eventFlow.emit(ChannelEvent.StateChanged(channelId, previous, newState, errorMessage))
    }

    override suspend fun reportAuthRequest(request: AuthRequest) {
        eventFlow.emit(ChannelEvent.AuthRequired(channelId, request))
    }

    override suspend fun notifyOutboundResult(messageId: String, success: Boolean, errorReason: String?) {
        // In-process pipe send is synchronous through channels; no extension ack path required.
    }

    override suspend fun sendMessage(message: ChannelMessage) {
        val isConnected = mutex.withLock { state == ChannelState.CONNECTED }
        if (!isConnected) {
            throw ChannelNotConnectedException(channelId)
        }
        inboundToExtension.send(message)
        eventFlow.emit(ChannelEvent.MessageSent(message))
    }

    /** Closes underlying channels to release resources. */
    suspend fun close() {
        mutex.withLock {
            state = ChannelState.UNREGISTERED
        }
        inboundToExtension.close()
        outboundFromExtension.close()
    }
}
