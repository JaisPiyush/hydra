package com.benzenelabs.hydra.host.io.connection.tcp

import com.benzenelabs.hydra.host.io.ConnectionHandle
import com.benzenelabs.hydra.host.io.ConnectionState
import com.benzenelabs.hydra.host.io.FrameType
import com.benzenelabs.hydra.host.io.IoFrame
import com.benzenelabs.hydra.host.io.connection.IoConnectException
import com.benzenelabs.hydra.host.io.connection.IoSendException
import com.benzenelabs.hydra.host.io.policy.ReconnectPolicy
import java.net.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * [TcpConnection] over a raw TCP socket.
 */
class TcpConnectionImpl(
    override var handle: ConnectionHandle,
    private val reconnectPolicy: ReconnectPolicy = ReconnectPolicy.DEFAULT,
    private val readBufferSize: Int = 8192,
    private val scope: CoroutineScope
) : TcpConnection {

    private val _frames = MutableSharedFlow<IoFrame>(extraBufferCapacity = 128)
    private val _state = MutableStateFlow(ConnectionState.IDLE)

    private var socket: Socket? = null
    private var closedGracefully = false
    private var reconnectAttempts = 0

    override val frames: Flow<IoFrame> = _frames
    override val stateFlow: Flow<ConnectionState> = _state

    override suspend fun open() {
        closedGracefully = false
        reconnectAttempts = 0
        transitionTo(ConnectionState.CONNECTING)
        connectNow()
    }

    override suspend fun send(data: ByteArray) = withContext(Dispatchers.IO) {
        check(_state.value == ConnectionState.CONNECTED) {
            "Cannot send on a ${_state.value} TCP connection"
        }
        try {
            val out = socket?.getOutputStream() ?: throw IoSendException(handle.id)
            out.write(data)
            out.flush()
        } catch (e: Exception) {
            throw IoSendException(handle.id, e)
        }
    }

    override suspend fun close() = withContext(Dispatchers.IO) {
        closedGracefully = true
        socket?.close()
        transitionTo(ConnectionState.CLOSED)
    }

    private suspend fun connectNow() = withContext(Dispatchers.IO) {
        val (host, port) = parseAddress(handle.remoteAddress!!)
        try {
            socket = Socket(host, port)
            reconnectAttempts = 0
            transitionTo(ConnectionState.CONNECTED)
            startReadLoop()
        } catch (e: Exception) {
            throw IoConnectException(handle.remoteAddress ?: "", e)
        }
    }

    private fun startReadLoop() {
        scope.launch(Dispatchers.IO) {
            val stream = socket?.getInputStream() ?: return@launch
            val buffer = ByteArray(readBufferSize)
            try {
                while (isActive) {
                    val read = stream.read(buffer)
                    if (read == -1) break
                    _frames.emit(
                        IoFrame(
                            connectionId = handle.id,
                            payload = buffer.copyOf(read),
                            frameType = FrameType.BINARY,
                            receivedAt = System.currentTimeMillis()
                        )
                    )
                }
            } catch (_: Exception) {
                // Read loop failure handled by reconnect logic below.
            }
            if (!closedGracefully) {
                scheduleReconnect()
            } else {
                transitionTo(ConnectionState.CLOSED)
            }
        }
    }

    private fun scheduleReconnect() {
        if (closedGracefully) return
        transitionTo(ConnectionState.RECONNECTING)
        scope.launch {
            reconnectAttempts += 1
            if (!reconnectPolicy.shouldRetry(reconnectAttempts)) {
                transitionTo(ConnectionState.FAILED)
                return@launch
            }
            delay(reconnectPolicy.delayFor(reconnectAttempts))
            transitionTo(ConnectionState.CONNECTING)
            connectNow()
        }
    }

    private fun transitionTo(next: ConnectionState) {
        if (_state.value.canTransitionTo(next)) {
            _state.value = next
            handle = handle.withState(next)
        }
    }

    internal fun parseAddress(url: String): Pair<String, Int> {
        require(url.startsWith("tcp://")) { "Invalid TCP address: $url" }
        val hostPort = url.removePrefix("tcp://")
        val idx = hostPort.lastIndexOf(':')
        require(idx > 0) { "Invalid TCP address: $url" }
        val host = hostPort.substring(0, idx)
        val port = hostPort.substring(idx + 1).toIntOrNull()
            ?: throw IllegalArgumentException("Invalid port in: $url")
        return host to port
    }
}
