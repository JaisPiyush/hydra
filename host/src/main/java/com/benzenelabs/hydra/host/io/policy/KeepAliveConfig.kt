package com.benzenelabs.hydra.host.io.policy

/**
 * WebSocket keep-alive configuration.
 */
data class KeepAliveConfig(
    val pingIntervalMs: Long = 30_000L,
    val pongTimeoutMs: Long = 10_000L
) {
    init {
        require(pingIntervalMs >= 0) { "pingIntervalMs must be >= 0" }
        if (pingIntervalMs > 0) {
            require(pongTimeoutMs > 0) { "pongTimeoutMs must be > 0 when ping is enabled" }
            require(pongTimeoutMs < pingIntervalMs) {
                "pongTimeoutMs must be < pingIntervalMs"
            }
        }
    }

    /** Whether keep-alive pinging is enabled. */
    val isEnabled: Boolean get() = pingIntervalMs > 0

    companion object {
        /** Default keep-alive profile. */
        val DEFAULT = KeepAliveConfig()

        /** Keep-alive disabled. */
        val DISABLED = KeepAliveConfig(pingIntervalMs = 0L, pongTimeoutMs = 0L)
    }
}
