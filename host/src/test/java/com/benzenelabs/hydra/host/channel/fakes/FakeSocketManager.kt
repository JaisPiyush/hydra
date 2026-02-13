package com.benzenelabs.hydra.host.channel.io

import com.benzenelabs.hydra.host.channel.ChannelId
import com.benzenelabs.hydra.host.channel.ConnectionEvent
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class FakeSocketManager : SocketManager {

    val sockets = mutableMapOf<String, SocketHandle>()
    val sentData = mutableListOf<Pair<String, ByteArray>>()
    val sentText = mutableListOf<Pair<String, String>>()
    val closedSockets = mutableListOf<String>()

    private val _connectionEvents = MutableSharedFlow<ConnectionEvent>()

    override suspend fun connect(
            channelId: ChannelId,
            url: String,
            protocol: SocketProtocol,
            headers: Map<String, String>
    ): SocketHandle {
        val id = UUID.randomUUID().toString()
        val handle = SocketHandle(id, url, protocol)
        sockets[id] = handle
        _connectionEvents.emit(ConnectionEvent.Opened(id))
        return handle
    }

    override suspend fun send(handle: SocketHandle, data: ByteArray) {
        if (!sockets.containsKey(handle.id)) throw SocketNotFoundException(handle.id)
        sentData.add(handle.id to data)
    }

    override suspend fun sendText(handle: SocketHandle, text: String) {
        if (!sockets.containsKey(handle.id)) throw SocketNotFoundException(handle.id)
        if (handle.protocol != SocketProtocol.WEBSOCKET)
                throw UnsupportedOperationException("Not WebSocket")
        sentText.add(handle.id to text)
    }

    override suspend fun close(handle: SocketHandle, reason: String) {
        sockets.remove(handle.id)
        closedSockets.add(handle.id)
        _connectionEvents.emit(ConnectionEvent.Closed(handle.id, 1000, reason))
    }

    override suspend fun closeAll(channelId: ChannelId) {
        // In fake, we don't track channelId per socket in the map efficiently,
        // but for testing we can just clear all or assume caller manages it.
        // For strict correctness, we should track it.
        // But implementing full logic here is overkill for a simple fake if not testing
        // multi-channel.
        val toClose = sockets.keys.toList() // Close all for equality
        toClose.forEach { id ->
            sockets.remove(id)
            closedSockets.add(id)
            _connectionEvents.emit(ConnectionEvent.Closed(id, 1000, "Closed All"))
        }
    }

    override fun connectionEvents(channelId: ChannelId): Flow<ConnectionEvent> =
            _connectionEvents.asSharedFlow()
}
