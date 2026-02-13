package com.benzenelabs.hydra.host.io

import com.benzenelabs.hydra.host.channel.ChannelId
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ConnectionHandleTest {

    private val chId = ChannelId("ch.123")

    @Test
    fun validations() {
        assertThrows(IllegalArgumentException::class.java) {
            ConnectionHandle("", ConnectionType.WEBSOCKET, chId, "ws://url", ConnectionState.IDLE)
        }

        // Invalid UUID
        assertThrows(IllegalArgumentException::class.java) {
            ConnectionHandle(
                    "not-uuid",
                    ConnectionType.WEBSOCKET,
                    chId,
                    "ws://url",
                    ConnectionState.IDLE
            )
        }

        // Null remote address for WEBSOCKET
        assertThrows(IllegalArgumentException::class.java) {
            ConnectionHandle(
                    UUID.randomUUID().toString(),
                    ConnectionType.WEBSOCKET,
                    chId,
                    null,
                    ConnectionState.IDLE
            )
        }

        // Null remote address for PIPE IS allowed
        val pipeHandle =
                ConnectionHandle(
                        UUID.randomUUID().toString(),
                        ConnectionType.PIPE,
                        chId,
                        null,
                        ConnectionState.IDLE
                )
        assertNotNull(pipeHandle)
    }

    @Test
    fun `withState returns copy`() {
        val h1 =
                ConnectionHandle(
                        UUID.randomUUID().toString(),
                        ConnectionType.WEBSOCKET,
                        chId,
                        "ws://url",
                        ConnectionState.IDLE
                )
        val h2 = h1.withState(ConnectionState.CONNECTING)

        assertEquals(ConnectionState.IDLE, h1.state)
        assertEquals(ConnectionState.CONNECTING, h2.state)
        assertEquals(h1.id, h2.id)
    }

    @Test
    fun `withState validates transition`() {
        val h1 =
                ConnectionHandle(
                        UUID.randomUUID().toString(),
                        ConnectionType.WEBSOCKET,
                        chId,
                        "ws://url",
                        ConnectionState.CLOSED
                )

        assertThrows(IllegalArgumentException::class.java) {
            h1.withState(ConnectionState.CONNECTING)
        }
    }

    @Test
    fun `generateId produces distinct uuids`() {
        val id1 = ConnectionHandle.generateId()
        val id2 = ConnectionHandle.generateId()
        assertNotEquals(id1, id2)
        UUID.fromString(id1) // should not throw
    }
}
