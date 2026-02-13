package com.benzenelabs.hydra.host.io.connection.longpoll

import com.benzenelabs.hydra.host.channel.ChannelId
import com.benzenelabs.hydra.host.data.StorageScope
import com.benzenelabs.hydra.host.data.config.ConfigField
import com.benzenelabs.hydra.host.data.config.ConfigStore
import com.benzenelabs.hydra.host.io.ConnectionHandle
import com.benzenelabs.hydra.host.io.ConnectionState
import com.benzenelabs.hydra.host.io.FrameType
import com.benzenelabs.hydra.host.io.IoFrame
import com.benzenelabs.hydra.host.io.connection.http.HttpClient
import com.benzenelabs.hydra.host.io.connection.http.HttpMethod
import com.benzenelabs.hydra.host.io.connection.http.HttpRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

/**
 * [LongPollConnection] that owns an HTTP GET poll loop.
 */
class LongPollConnectionImpl(
    override var handle: ConnectionHandle,
    private val config: LongPollConfig,
    private val channelId: ChannelId,
    private val configStore: ConfigStore,
    private val httpClient: HttpClient,
    private val scope: CoroutineScope
) : LongPollConnection {

    private val _frames = MutableSharedFlow<IoFrame>(extraBufferCapacity = 64)
    private val _state = MutableStateFlow(ConnectionState.IDLE)

    private var pollJob: Job? = null
    private val cursorField = ConfigField(name = config.cursorConfigKey)
    private val extensionScope = StorageScope.Extension(channelId)

    override val frames: Flow<IoFrame> = _frames
    override val stateFlow: Flow<ConnectionState> = _state

    override suspend fun open() {
        transitionTo(ConnectionState.CONNECTING)
        transitionTo(ConnectionState.CONNECTED)
        pollJob = scope.launch { runPollLoop() }
    }

    override suspend fun send(data: ByteArray): Nothing {
        throw UnsupportedOperationException(
            "LongPollConnection is receive-only. Use HttpClient for outbound requests."
        )
    }

    override suspend fun close() {
        if (pollJob != null) {
            pollJob!!.cancel()
        }
        pollJob = null
        transitionTo(ConnectionState.CLOSED)
    }

    private suspend fun runPollLoop() {
        var backoffMs = config.intervalMs
        while (_state.value == ConnectionState.CONNECTED) {
            currentCoroutineContext().ensureActive()
            val cursor = configStore.get(extensionScope, cursorField)
            val request = HttpRequest(
                url = config.resolvedUrl(cursor),
                method = HttpMethod.GET,
                headers = config.headers,
                timeoutMs = config.timeoutMs
            )
            try {
                val response = httpClient.execute(request)
                if (response.isSuccess && response.body.isNotEmpty()) {
                    _frames.emit(
                        IoFrame(
                            connectionId = handle.id,
                            payload = response.body,
                            frameType = FrameType.BINARY,
                            receivedAt = System.currentTimeMillis()
                        )
                    )
                    backoffMs = config.intervalMs
                }
                delay(config.intervalMs)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                delay(backoffMs)
                backoffMs = (backoffMs * 2).coerceAtMost(60_000L)
            }
        }
    }

    private fun transitionTo(next: ConnectionState) {
        if (_state.value.canTransitionTo(next)) {
            _state.value = next
            handle = handle.withState(next)
        }
    }
}
