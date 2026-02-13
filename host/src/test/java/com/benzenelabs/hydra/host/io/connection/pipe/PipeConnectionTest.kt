package com.benzenelabs.hydra.host.io.connection.pipe

import com.benzenelabs.hydra.host.channel.ChannelId
import com.benzenelabs.hydra.host.io.ConnectionHandle
import com.benzenelabs.hydra.host.io.ConnectionState
import com.benzenelabs.hydra.host.io.ConnectionType
import com.benzenelabs.hydra.host.io.FrameType
import com.benzenelabs.hydra.host.io.IoFrame
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PipeConnectionTest {

    @Test
    fun `pipe flow`() = runTest {
        val handle =
                ConnectionHandle(
                        id = UUID.randomUUID().toString(),
                        type = ConnectionType.PIPE,
                        channelId = ChannelId("ch.1"),
                        remoteAddress = null,
                        state = ConnectionState.IDLE
                )
        val connection = PipeConnection(handle)

        connection.open()

        assertEquals(ConnectionState.CONNECTED, connection.stateFlow.first())

        // Test outbound (Agent -> App)
        connection.sendText("Hello App")
        val outboundFrame = connection.outbound.receive()
        assertEquals("Hello App", outboundFrame.payloadAsText())

        // Test inbound (App -> Agent)
        connection.inbound.send(
                IoFrame(
                        handle.id,
                        "Hello Agent".toByteArray(),
                        FrameType.TEXT,
                        System.currentTimeMillis()
                )
        )
        val inboundFrame = connection.frames.first()
        assertEquals("Hello Agent", inboundFrame.payloadAsText())

        connection.close()
        assertEquals(ConnectionState.CLOSED, connection.stateFlow.first())
    }
}
