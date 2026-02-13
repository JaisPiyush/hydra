package com.benzenelabs.hydra.host.io

import com.benzenelabs.hydra.host.channel.ChannelId
import com.benzenelabs.hydra.host.channel.ConnectionEvent
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

/** [SocketManager] implementation backed by OkHttp WebSocket and java.net.Socket. */
class SocketManagerImpl(
        private val scope: CoroutineScope,
        private val okHttpClient: OkHttpClient = OkHttpClient()
) : SocketManager {

    private sealed class ManagedSocket {
        abstract val channelId: ChannelId
        abstract val handle: SocketHandle

        data class ManagedWebSocket(
                override val channelId: ChannelId,
                override val handle: SocketHandle,
                val socket: WebSocket
        ) : ManagedSocket()

        data class ManagedTcpSocket(
                override val channelId: ChannelId,
                override val handle: SocketHandle,
                val socket: Socket,
                val readerJob: Job
        ) : ManagedSocket()
    }

    private val sockets = ConcurrentHashMap<String, ManagedSocket>()
    private val channelEvents = ConcurrentHashMap<ChannelId, MutableSharedFlow<ConnectionEvent>>()

    override suspend fun connect(
            channelId: ChannelId,
            url: String,
            protocol: SocketProtocol,
            headers: Map<String, String>
    ): SocketHandle =
            withContext(Dispatchers.IO) {
                require(url.isNotBlank()) { "url must not be blank" }
                val handle =
                        SocketHandle(
                                id = UUID.randomUUID().toString(),
                                url = url,
                                protocol = protocol
                        )
                try {
                    when (protocol) {
                        SocketProtocol.WEBSOCKET -> connectWebSocket(channelId, handle, headers)
                        SocketProtocol.TCP -> connectTcp(channelId, handle)
                    }
                    handle
                } catch (e: SocketConnectException) {
                    throw e
                } catch (e: Exception) {
                    throw SocketConnectException(url, e)
                }
            }

    override suspend fun send(handle: SocketHandle, data: ByteArray) {
        withContext(Dispatchers.IO) {
            when (val socket = sockets[handle.id]) {
                null -> throw SocketNotFoundException(handle.id)
                is ManagedSocket.ManagedWebSocket -> {
                    val ok = socket.socket.send(ByteString.of(*data))
                    if (!ok) {
                        throw SocketSendException(handle.id)
                    }
                }
                is ManagedSocket.ManagedTcpSocket -> {
                    try {
                        socket.socket.getOutputStream().write(data)
                        socket.socket.getOutputStream().flush()
                    } catch (e: Exception) {
                        throw SocketSendException(handle.id, e)
                    }
                }
            }
        }
    }

    override suspend fun sendText(handle: SocketHandle, text: String) {
        withContext(Dispatchers.IO) {
            val socket = sockets[handle.id] ?: throw SocketNotFoundException(handle.id)
            if (socket !is ManagedSocket.ManagedWebSocket) {
                throw UnsupportedOperationException("Text frames are only supported for WEBSOCKET")
            }
            val ok = socket.socket.send(text)
            if (!ok) {
                throw SocketSendException(handle.id)
            }
        }
    }

    override suspend fun close(handle: SocketHandle, reason: String) {
        withContext(Dispatchers.IO) {
            val socket = sockets.remove(handle.id) ?: return@withContext
            when (socket) {
                is ManagedSocket.ManagedWebSocket -> {
                    socket.socket.close(1000, reason)

                }
                is ManagedSocket.ManagedTcpSocket -> {
                    socket.readerJob.cancel()
                    socket.socket.close()
                    emitChannelEvent(
                            socket.channelId,
                            ConnectionEvent.Closed(handle.id, 1000, reason)
                    )
                }
            }
        }
    }

    override suspend fun closeAll(channelId: ChannelId) {
        withContext(Dispatchers.IO) {
            val handles = sockets.values.filter { it.channelId == channelId }.map { it.handle }
            handles.forEach { close(it) }
        }
    }

    override fun connectionEvents(channelId: ChannelId): Flow<ConnectionEvent> =
            eventsForChannel(channelId).asSharedFlow()

    private fun connectWebSocket(
            channelId: ChannelId,
            handle: SocketHandle,
            headers: Map<String, String>
    ) {
        val requestBuilder = Request.Builder().url(handle.url)
        headers.forEach { (key, value) -> requestBuilder.addHeader(key, value) }

        val webSocket =
                okHttpClient.newWebSocket(
                        requestBuilder.build(),
                        object : WebSocketListener() {
                            override fun onOpen(webSocket: WebSocket, response: Response) {
                                sockets[handle.id] =
                                        ManagedSocket.ManagedWebSocket(channelId, handle, webSocket)
                                emitChannelEvent(channelId, ConnectionEvent.Opened(handle.id))
                            }

                            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                                sockets.remove(handle.id)
                                emitChannelEvent(
                                        channelId,
                                        ConnectionEvent.Closed(handle.id, code, reason)
                                )
                            }

                            override fun onFailure(
                                    webSocket: WebSocket,
                                    t: Throwable,
                                    response: Response?
                            ) {
                                sockets.remove(handle.id)
                                emitChannelEvent(channelId, ConnectionEvent.Error(handle.id, t))
                            }
                        }
                )
//        sockets[handle.id] = ManagedSocket.ManagedWebSocket(channelId, handle, webSocket)
    }

    private fun connectTcp(channelId: ChannelId, handle: SocketHandle) {
        val uri =
                try {
                    URI(handle.url)
                } catch (e: Exception) {
                    throw SocketConnectException(handle.url, e)
                }
        if (uri.scheme != "tcp") {
            throw SocketConnectException(
                    handle.url,
                    IllegalArgumentException("TCP URL must use tcp:// scheme")
            )
        }
        val host =
                uri.host
                        ?: throw SocketConnectException(
                                handle.url,
                                IllegalArgumentException("Missing host")
                        )
        val port =
                if (uri.port > 0) uri.port
                else
                        throw SocketConnectException(
                                handle.url,
                                IllegalArgumentException("Missing port")
                        )

        val socket = Socket()
        socket.connect(InetSocketAddress(host, port))

        val readerJob =
                scope.launch(Dispatchers.IO) {
                    val buffer = ByteArray(DEFAULT_TCP_BUFFER_SIZE)
                    try {
                        val input = socket.getInputStream()
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) {
                                break
                            }
                        }
                        emitChannelEvent(channelId, ConnectionEvent.Closed(handle.id, 1000, "EOF"))
                    } catch (e: Exception) {
                        emitChannelEvent(channelId, ConnectionEvent.Error(handle.id, e))
                    } finally {
                        sockets.remove(handle.id)
                        socket.close()
                    }
                }

        sockets[handle.id] = ManagedSocket.ManagedTcpSocket(channelId, handle, socket, readerJob)
        emitChannelEvent(channelId, ConnectionEvent.Opened(handle.id))
    }

    private fun eventsForChannel(channelId: ChannelId): MutableSharedFlow<ConnectionEvent> =
            channelEvents.getOrPut(channelId) { MutableSharedFlow(extraBufferCapacity = 128) }

    private fun emitChannelEvent(channelId: ChannelId, event: ConnectionEvent) {
        val flow = eventsForChannel(channelId)
        if (!flow.tryEmit(event)) {
            scope.launch { flow.emit(event) }
        }
    }



    private companion object {
        private const val DEFAULT_TCP_BUFFER_SIZE = 8 * 1024
    }
}
