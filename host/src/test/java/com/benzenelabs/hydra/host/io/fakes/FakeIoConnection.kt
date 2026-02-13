package com.benzenelabs.hydra.host.io.fakes

import com.benzenelabs.hydra.host.io.ConnectionHandle
import com.benzenelabs.hydra.host.io.ConnectionState
import com.benzenelabs.hydra.host.io.IoFrame
import com.benzenelabs.hydra.host.io.connection.IoConnection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeIoConnection(override var handle: ConnectionHandle) : IoConnection {

    private val _frames = MutableSharedFlow<IoFrame>(extraBufferCapacity = 64)
    private val _state = MutableStateFlow(ConnectionState.IDLE)

    val sentData = mutableListOf<ByteArray>()
    val sentText = mutableListOf<String>()

    override val frames: Flow<IoFrame> = _frames
    override val stateFlow: Flow<ConnectionState> = _state

    override suspend fun open() {
        transitionTo(ConnectionState.CONNECTED)
    }

    override suspend fun send(data: ByteArray) {
        check(_state.value == ConnectionState.CONNECTED) { "Not connected" }
        sentData.add(data)
    }

    override suspend fun sendText(text: String) {
        check(_state.value == ConnectionState.CONNECTED) { "Not connected" }
        sentText.add(text)
        // Also add to sentData as default implementation does
        sentData.add(text.toByteArray(Charsets.UTF_8))
    }

    override suspend fun close() {
        transitionTo(ConnectionState.CLOSED)
    }

    fun emitFrame(frame: IoFrame) {
        _frames.tryEmit(frame)
    }

    fun setState(state: ConnectionState) {
        transitionTo(state)
    }

    fun simulateSendError() {
        // Checking state is correct in test setup usually, but we could make send() throw if a flag
        // is set
    }

    // Helper for tests to simulate failure during send
    var shouldFailSend = false

    private fun transitionTo(next: ConnectionState) {
        _state.value = next
        handle = handle.withState(next)
    }
}
