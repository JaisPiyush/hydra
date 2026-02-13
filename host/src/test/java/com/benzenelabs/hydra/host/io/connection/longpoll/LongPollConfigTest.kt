package com.benzenelabs.hydra.host.io.connection.longpoll

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LongPollConfigTest {
    @Test
    fun `validations`() {
        assertThrows(IllegalArgumentException::class.java) { LongPollConfig(url = "") }
        assertThrows(IllegalArgumentException::class.java) {
            LongPollConfig(url = "http://url", intervalMs = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            LongPollConfig(url = "http://url", timeoutMs = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            LongPollConfig(url = "http://url", cursorConfigKey = "")
        }
    }

    @Test
    fun `resolvedUrl substitutes cursor`() {
        val config = LongPollConfig(url = "http://api.com/updates?since={cursor}")

        assertEquals("http://api.com/updates?since=123", config.resolvedUrl("123"))
        assertEquals("http://api.com/updates?since={cursor}", config.resolvedUrl(null))
    }

    @Test
    fun `resolvedUrl ignores if no placeholder`() {
        val config = LongPollConfig(url = "http://api.com/updates")

        assertEquals("http://api.com/updates", config.resolvedUrl("123"))
    }
}
