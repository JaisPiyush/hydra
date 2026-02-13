package com.benzenelabs.hydra.host.io.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ReconnectPolicyTest {
    @Test
    fun `validations`() {
        assertThrows(IllegalArgumentException::class.java) { ReconnectPolicy(maxAttempts = -1) }
        assertThrows(IllegalArgumentException::class.java) { ReconnectPolicy(initialDelayMs = 0) }
        assertThrows(IllegalArgumentException::class.java) {
            // max < initial
            ReconnectPolicy(initialDelayMs = 2000, maxDelayMs = 1000)
        }
        assertThrows(IllegalArgumentException::class.java) { ReconnectPolicy(jitter = 1.5f) }
    }

    @Test
    fun `shouldRetry respects maxAttempts`() {
        val policy = ReconnectPolicy(maxAttempts = 3)
        assertTrue(policy.shouldRetry(1))
        assertTrue(policy.shouldRetry(2))
        assertTrue(policy.shouldRetry(3))
        assertFalse(policy.shouldRetry(4))
    }

    @Test
    fun `shouldRetry unlimited`() {
        val policy = ReconnectPolicy(maxAttempts = 0)
        assertTrue(policy.shouldRetry(100))
    }

    @Test
    fun `delay growth without jitter`() {
        val policy =
                ReconnectPolicy(
                        initialDelayMs = 100,
                        multiplier = 2f,
                        maxDelayMs = 1000,
                        jitter = 0f
                )

        // 1: 100 * 2^0 = 100
        assertEquals(100L, policy.delayFor(1))
        // 2: 100 * 2^1 = 200
        assertEquals(200L, policy.delayFor(2))
        // 3: 100 * 2^2 = 400
        assertEquals(400L, policy.delayFor(3))
        // 4: 100 * 2^3 = 800
        assertEquals(800L, policy.delayFor(4))
        // 5: 100 * 2^4 = 1600 -> capped to 1000
        assertEquals(1000L, policy.delayFor(5))
    }

    @Test
    fun `delay with jitter is within range`() {
        val initial = 1000L
        val jitter = 0.2f // +/- 20%
        val policy = ReconnectPolicy(initialDelayMs = initial, jitter = jitter)

        val delay = policy.delayFor(1)

        // range: 800 .. 1200
        val min = (initial * (1 - jitter)).toLong()
        val max = (initial * (1 + jitter)).toLong()

        // Note: The jitter formula in main code is: base + (base * jitter * random(-1, 1))
        // random(-1, 1) means we could subtract or add up to jitter amount.

        assertTrue("Delay $delay should be >= $min", delay >= min)
        assertTrue("Delay $delay should be <= $max", delay <= max)
    }
}
