package com.benzenelabs.hydra.host.io

import com.benzenelabs.hydra.host.io.SocketHandle
import com.benzenelabs.hydra.host.io.SocketProtocol
import java.util.UUID
import org.junit.Test

class SocketHandleTest {

    @Test
    fun `valid construction`() {
        SocketHandle(UUID.randomUUID().toString(), "wss://echo", SocketProtocol.WEBSOCKET)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank id throws`() {
        SocketHandle("  ", "url", SocketProtocol.TCP)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid UUID throws`() {
        SocketHandle("not-uuid", "url", SocketProtocol.TCP)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank url throws`() {
        SocketHandle(UUID.randomUUID().toString(), "  ", SocketProtocol.TCP)
    }
}
