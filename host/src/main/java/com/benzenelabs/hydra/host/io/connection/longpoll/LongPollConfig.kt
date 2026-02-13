package com.benzenelabs.hydra.host.io.connection.longpoll

/**
 * Configuration for host-managed HTTP long-poll connections.
 */
data class LongPollConfig(
    val url: String,
    val intervalMs: Long = 5_000L,
    val cursorConfigKey: String = "poll_cursor",
    val headers: Map<String, String> = emptyMap(),
    val timeoutMs: Long = 30_000L
) {
    init {
        require(url.isNotBlank()) { "LongPollConfig.url must not be blank" }
        require(intervalMs > 0) { "LongPollConfig.intervalMs must be > 0" }
        require(timeoutMs > 0) { "LongPollConfig.timeoutMs must be > 0" }
        require(cursorConfigKey.isNotBlank()) {
            "LongPollConfig.cursorConfigKey must not be blank"
        }
    }

    /** Substitutes optional {cursor} in [url]. */
    fun resolvedUrl(cursor: String?): String =
        if (cursor != null) url.replace("{cursor}", cursor) else url
}
