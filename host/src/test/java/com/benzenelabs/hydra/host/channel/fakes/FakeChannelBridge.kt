package com.benzenelabs.hydra.host.channel.fakes

import com.benzenelabs.hydra.host.channel.AuthRequest
import com.benzenelabs.hydra.host.channel.ChannelBridge
import com.benzenelabs.hydra.host.channel.ChannelEvent
import com.benzenelabs.hydra.host.channel.ChannelId
import com.benzenelabs.hydra.host.channel.ChannelMessage
import com.benzenelabs.hydra.host.channel.ChannelState
import com.benzenelabs.hydra.host.channel.ConnectionEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class FakeChannelBridge(override val channelId: ChannelId) : ChannelBridge {

    private val _events = MutableSharedFlow<ChannelEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<ChannelEvent> = _events.asSharedFlow()

    // Recording calls
    val deliveredToExtension = mutableListOf<Pair<String, ByteArray>>()
    val notifiedConnectionEvents = mutableListOf<ConnectionEvent>()
    val emittedInboundMessages = mutableListOf<ChannelMessage>()
    val reportedStateChanges = mutableListOf<Pair<ChannelState, String?>>()
    val reportedAuthRequests = mutableListOf<AuthRequest>()
    val notifiedOutboundResults = mutableListOf<Triple<String, Boolean, String?>>()
    val sentMessages = mutableListOf<ChannelMessage>()

    var closed = false

    override suspend fun deliverToExtension(socketId: String, data: ByteArray) {
        deliveredToExtension.add(socketId to data)
    }

    override suspend fun notifyConnectionEvent(event: ConnectionEvent) {
        notifiedConnectionEvents.add(event)
    }

    override suspend fun emitInboundMessage(message: ChannelMessage) {
        emittedInboundMessages.add(message)
        _events.emit(ChannelEvent.MessageReceived(message))
    }

    override suspend fun reportStateChange(newState: ChannelState, errorMessage: String?) {
        reportedStateChanges.add(newState to errorMessage)
        // In a real bridge, this might trigger a host callback, but here we just record it.
        // The Registry observes 'events' from the bridge?
        // No, Registry observes 'events' which are emitted *by the bridge*.
        // But 'reportStateChange' typically comes *from* JS and might cause the Bridge to emit a
        // StateChanged event *if* the bridge is responsible for that.
        // However, in the architecture, Registry manages state.
        // The Bridge interface says: "Called by the JS extension ... to update the host-side
        // ChannelState machine."
        // Usually, the bridge would then call Registry.updateState.
        // Or if the Bridge emits an event?
        // Let's assume for this Fake, we just record.
    }

    override suspend fun reportAuthRequest(request: AuthRequest) {
        reportedAuthRequests.add(request)
        _events.emit(ChannelEvent.AuthRequired(channelId, request))
    }

    override suspend fun notifyOutboundResult(
            messageId: String,
            success: Boolean,
            errorReason: String?
    ) {
        notifiedOutboundResults.add(Triple(messageId, success, errorReason))
        if (!success) {
            _events.emit(
                    ChannelEvent.MessageSendFailed(
                            com.benzenelabs.hydra.host.data.session.SessionId(
                                    "unknown"
                            ), // Simplified for fake
                            channelId,
                            errorReason ?: "Unknown",
                            null
                    )
            )
        } else {
            // Maybe emit MessageSent?
        }
    }

    override suspend fun sendMessage(message: ChannelMessage) {
        sentMessages.add(message)
        _events.emit(ChannelEvent.MessageSent(message))
    }

    // Identify as closable for Registry tests
    override suspend fun close() {
        closed = true
    }
}
