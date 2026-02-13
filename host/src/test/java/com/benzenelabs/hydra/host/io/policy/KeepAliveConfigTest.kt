package com.benzenelabs.hydra.host.io.policy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class KeepAliveConfigTest {
    @Test
    fun `validations`() {
        assertThrows(IllegalArgumentException::class.java) { KeepAliveConfig(pingIntervalMs = -1) }

        // Enabled ping requires valid timeout
        assertThrows(IllegalArgumentException::class.java) {
            KeepAliveConfig(pingIntervalMs = 1000, pongTimeoutMs = 0)
        }

        // Timeout must be < Interval
        assertThrows(IllegalArgumentException::class.java) {
            KeepAliveConfig(pingIntervalMs = 1000, pongTimeoutMs = 1000)
        }
        assertThrows(IllegalArgumentException::class.java) {
            KeepAliveConfig(pingIntervalMs = 1000, pongTimeoutMs = 2000)
        }
    }

    @Test
    fun `isEnabled logic`() {
        assertTrue(KeepAliveConfig.DEFAULT.isEnabled)
        assertFalse(KeepAliveConfig.DISABLED.isEnabled)

        val custom = KeepAliveConfig(pingIntervalMs = 5000, pongTimeoutMs = 1000)
        assertTrue(custom.isEnabled)
    }
}
