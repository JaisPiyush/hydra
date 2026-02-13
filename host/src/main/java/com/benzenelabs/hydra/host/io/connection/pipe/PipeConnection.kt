package com.benzenelabs.hydra.host.io.connection.pipe

import com.benzenelabs.hydra.host.io.ConnectionHandle
import com.benzenelabs.hydra.host.io.ConnectionState
import com.benzenelabs.hydra.host.io.FrameType
import com.benzenelabs.hydra.host.io.IoFrame
import com.benzenelabs.hydra.host.io.connection.IoConnection
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.consumeAsFlow

/**
 * In-process [IoConnection] backed by coroutine [Channel]s.
 */
class PipeConnection(
    override var handle: ConnectionHandle,
    capacity: Int = 64
) : IoConnection {

    /** Write here to simulate inbound messages for the agent pipeline. */
    val inbound: Channel<IoFrame> = Channel(capacity)

    /** Read here to consume outbound frames produced by the agent pipeline. */
    val outbound: Channel<IoFrame> = Channel(capacity)

    private val _state = MutableStateFlow(ConnectionState.IDLE)

    override val frames: Flow<IoFrame> = inbound.consumeAsFlow()
    override val stateFlow: Flow<ConnectionState> = _state

    override suspend fun open() {
        transitionTo(ConnectionState.CONNECTING)
        transitionTo(ConnectionState.CONNECTED)
    }

    override suspend fun send(data: ByteArray) {
        check(_state.value == ConnectionState.CONNECTED) {
            "Cannot send on a ${_state.value} pipe connection"
        }
        outbound.send(
            IoFrame(
                connectionId = handle.id,
                payload = data,
                frameType = FrameType.BINARY,
                receivedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun close() {
        inbound.close()
        outbound.close()
        transitionTo(ConnectionState.CLOSED)
    }

    private fun transitionTo(next: ConnectionState) {
        if (_state.value.canTransitionTo(next)) {
            _state.value = next
            handle = handle.withState(next)
        }
    }
}
