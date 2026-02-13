package com.benzenelabs.hydra.host.data.session

/**
 * Lifecycle state of a [Session].
 *
 * Transitions:
 *   PENDING -> ACTIVE -> SUSPENDED -> ACTIVE  (resume)
 *   ACTIVE -> CLOSED
 *   PENDING | ACTIVE -> CLOSED
 */
enum class SessionState {
    /** Created but not yet acknowledged by the channel extension. */
    PENDING,

    /** Actively in use - messages are being exchanged. */
    ACTIVE,

    /** Temporarily suspended (e.g., app backgrounded, connection dropped). */
    SUSPENDED,

    /** Permanently closed. No further messages will be processed. */
    CLOSED;

    fun canTransitionTo(next: SessionState): Boolean = when (this) {
        PENDING -> next == ACTIVE || next == CLOSED
        ACTIVE -> next == SUSPENDED || next == CLOSED
        SUSPENDED -> next == ACTIVE || next == CLOSED
        CLOSED -> false
    }
}
