package com.benzenelabs.hydra.host.io

import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectionTypeTest {
    @Test
    fun `all types exist`() {
        assertEquals(5, ConnectionType.values().size)
        ConnectionType.valueOf("WEBSOCKET")
        ConnectionType.valueOf("TCP")
        ConnectionType.valueOf("LONG_POLL")
        ConnectionType.valueOf("PIPE")
        ConnectionType.valueOf("HTTP")
    }
}
