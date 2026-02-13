package com.benzenelabs.hydra.host.io.connection.websocket

import com.benzenelabs.hydra.host.io.ConnectionHandle
import com.benzenelabs.hydra.host.io.ConnectionState
import com.benzenelabs.hydra.host.io.FrameType
import com.benzenelabs.hydra.host.io.IoFrame
import com.benzenelabs.hydra.host.io.connection.IoConnectException
import com.benzenelabs.hydra.host.io.connection.IoSendException
import com.benzenelabs.hydra.host.io.policy.KeepAliveConfig
import com.benzenelabs.hydra.host.io.policy.ReconnectPolicy
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString

/**
 * [WebSocketConnection] backed by OkHttp [WebSocket].
 */
class WebSocketConnectionImpl(
    override var handle: ConnectionHandle,
    private val httpHeaders: Map<String, String> = emptyMap(),
    private val reconnectPolicy: ReconnectPolicy = ReconnectPolicy.DEFAULT,
    private val keepAlive: KeepAliveConfig = KeepAliveConfig.DEFAULT,
    private val okHttpClient: OkHttpClient,
    private val scope: CoroutineScope
) : WebSocketConnection {

    private val _frames = MutableSharedFlow<IoFrame>(extraBufferCapacity = 64)
    private val _state = MutableStateFlow(ConnectionState.IDLE)

    private val socketClient: OkHttpClient = if (keepAlive.isEnabled) {
        okHttpClient.newBuilder()
            .pingInterval(keepAlive.pingIntervalMs, TimeUnit.MILLISECONDS)
            .build()
    } else {
        okHttpClient
    }

    private var webSocket: WebSocket? = null
    private var reconnectAttempts = 0
    private var closedGracefully = false

    override val frames: Flow<IoFrame> = _frames
    override val stateFlow: Flow<ConnectionState> = _state

    override suspend fun open() {
        closedGracefully = false
        reconnectAttempts = 0
        transitionTo(ConnectionState.CONNECTING)

        val connected = CompletableDeferred<Result<Unit>>()
        try {
            connectNow(connected)
            connected.await().getOrThrow()
        } catch (t: Throwable) {
            transitionTo(ConnectionState.FAILED)
            throw IoConnectException(handle.remoteAddress ?: "", t)
        }
    }

    override suspend fun send(data: ByteArray) {
        check(_state.value == ConnectionState.CONNECTED) {
            "Cannot send on a ${_state.value} WebSocket connection"
        }
        val ok = webSocket?.send(data.toByteString()) ?: false
        if (!ok) throw IoSendException(handle.id)
    }

    override suspend fun sendText(text: String) {
        check(_state.value == ConnectionState.CONNECTED) {
            "Cannot send on a ${_state.value} WebSocket connection"
        }
        val ok = webSocket?.send(text) ?: false
        if (!ok) throw IoSendException(handle.id)
    }

    override suspend fun close() {
        closedGracefully = true
        webSocket?.close(1000, "Normal closure")
        transitionTo(ConnectionState.CLOSED)
    }

    private fun connectNow(initialConnect: CompletableDeferred<Result<Unit>>?) {
        val request = Request.Builder()
            .url(handle.remoteAddress!!)
            .apply { httpHeaders.forEach { (k, v) -> addHeader(k, v) } }
            .build()

        socketClient.newWebSocket(request, Listener(initialConnect))
    }

    private fun transitionTo(next: ConnectionState) {
        if (_state.value.canTransitionTo(next)) {
            _state.value = next
            handle = handle.withState(next)
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
            connectNow(initialConnect = null)
        }
    }

    private inner class Listener(
        private val initialConnect: CompletableDeferred<Result<Unit>>?
    ) : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            this@WebSocketConnectionImpl.webSocket = webSocket
            reconnectAttempts = 0
            transitionTo(ConnectionState.CONNECTED)
            initialConnect?.complete(Result.success(Unit))
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            _frames.tryEmit(
                IoFrame(
                    connectionId = handle.id,
                    payload = bytes.toByteArray(),
                    frameType = FrameType.BINARY,
                    receivedAt = System.currentTimeMillis()
                )
            )
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            _frames.tryEmit(
                IoFrame(
                    connectionId = handle.id,
                    payload = text.toByteArray(Charsets.UTF_8),
                    frameType = FrameType.TEXT,
                    receivedAt = System.currentTimeMillis()
                )
            )
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (!closedGracefully) {
                scheduleReconnect()
            } else {
                transitionTo(ConnectionState.CLOSED)
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (initialConnect != null && !initialConnect.isCompleted) {
                initialConnect.complete(Result.failure(t))
                return
            }
            scheduleReconnect()
        }
    }
}
