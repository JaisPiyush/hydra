package com.benzenelabs.hydra.host.io.connection.longpoll

/**
 * Opaque long-poll cursor value persisted per channel.
 */
@JvmInline
value class LongPollCursor(val value: String?) {
    companion object {
        /** First run: no persisted cursor. */
        val INITIAL = LongPollCursor(null)
    }
}
