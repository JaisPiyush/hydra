package com.benzenelabs.hydra.host.io.policy

import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Exponential backoff reconnect policy for persistent connections.
 */
data class ReconnectPolicy(
    val maxAttempts: Int = 0,
    val initialDelayMs: Long = 1_000L,
    val maxDelayMs: Long = 60_000L,
    val multiplier: Float = 2.0f,
    val jitter: Float = 0.2f
) {
    init {
        require(maxAttempts >= 0) { "maxAttempts must be >= 0" }
        require(initialDelayMs > 0) { "initialDelayMs must be > 0" }
        require(maxDelayMs >= initialDelayMs) { "maxDelayMs must be >= initialDelayMs" }
        require(multiplier >= 1.0f) { "multiplier must be >= 1.0" }
        require(jitter in 0.0f..1.0f) { "jitter must be in [0.0, 1.0]" }
    }

    /** Returns true if another retry is allowed. [attemptNumber] is 1-based. */
    fun shouldRetry(attemptNumber: Int): Boolean =
        maxAttempts == 0 || attemptNumber <= maxAttempts

    /** Returns delay in ms before [attemptNumber]. [attemptNumber] is 1-based. */
    fun delayFor(attemptNumber: Int): Long {
        require(attemptNumber >= 1) { "attemptNumber must be >= 1" }
        val base = min(
            initialDelayMs * multiplier.pow(attemptNumber - 1).toLong(),
            maxDelayMs
        )
        val variance = (base * jitter * (Random.nextFloat() * 2f - 1f)).toLong()
        return (base + variance).coerceAtLeast(0L)
    }

    companion object {
        /** Balanced default backoff. */
        val DEFAULT = ReconnectPolicy()

        /** Low-latency reconnect profile. */
        val FAST = ReconnectPolicy(
            initialDelayMs = 250L,
            maxDelayMs = 5_000L,
            multiplier = 1.5f,
            jitter = 0.1f
        )

        /** Disable reconnect loops (single attempt). */
        val NONE = ReconnectPolicy(maxAttempts = 1, initialDelayMs = 1L, maxDelayMs = 1L)
    }
}
