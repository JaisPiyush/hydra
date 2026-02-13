package com.benzenelabs.hydra.host.io

/**
 * Lifecycle state of a managed I/O connection.
 *
 * Transitions:
 * IDLE -> CONNECTING -> CONNECTED -> RECONNECTING -> CONNECTING (retry)
 *                              -> CLOSED (graceful)
 *                              -> FAILED (terminal)
 */
enum class ConnectionState {
    IDLE,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    CLOSED,
    FAILED;

    /** Returns true when this state is terminal. */
    fun isTerminal(): Boolean = this == CLOSED || this == FAILED

    /** Returns true if transition to [next] is valid. */
    fun canTransitionTo(next: ConnectionState): Boolean = when (this) {
        IDLE -> next == CONNECTING || next == CLOSED
        CONNECTING -> next == CONNECTED || next == RECONNECTING || next == FAILED || next == CLOSED
        CONNECTED -> next == RECONNECTING || next == CLOSED || next == FAILED
        RECONNECTING -> next == CONNECTING || next == CLOSED || next == FAILED
        CLOSED -> false
        FAILED -> false
    }
}
