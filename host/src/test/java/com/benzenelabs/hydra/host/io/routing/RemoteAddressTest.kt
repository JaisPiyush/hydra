package com.benzenelabs.hydra.host.io.routing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RemoteAddressTest {
    @Test
    fun `validations`() {
        assertThrows(IllegalArgumentException::class.java) { RemoteAddress("", 8080) }
        assertThrows(IllegalArgumentException::class.java) { RemoteAddress("host", 0) }
        assertThrows(IllegalArgumentException::class.java) { RemoteAddress("host", 65536) }

        RemoteAddress("host", 1) // valid
        RemoteAddress("host", 65535) // valid
    }

    @Test
    fun `parse valid`() {
        val addr = RemoteAddress.parse("192.168.1.1:8080")
        assertEquals("192.168.1.1", addr.host)
        assertEquals(8080, addr.port)
    }

    @Test
    fun `parse invalid`() {
        assertThrows(IllegalArgumentException::class.java) { RemoteAddress.parse("host-no-port") }
        assertThrows(IllegalArgumentException::class.java) { RemoteAddress.parse("host:abc") }
    }

    @Test
    fun `toString formats correctly`() {
        assertEquals("host:80", RemoteAddress("host", 80).toString())
    }
}
