package com.benzenelabs.hydra.host.io

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionStateTest {
    @Test
    fun `isTerminal returns true only for CLOSED and FAILED`() {
        assertFalse(ConnectionState.IDLE.isTerminal())
        assertFalse(ConnectionState.CONNECTING.isTerminal())
        assertFalse(ConnectionState.CONNECTED.isTerminal())
        assertFalse(ConnectionState.RECONNECTING.isTerminal())
        assertTrue(ConnectionState.CLOSED.isTerminal())
        assertTrue(ConnectionState.FAILED.isTerminal())
    }

    @Test
    fun `valid transitions`() {
        // IDLE transitions
        assertTrue(ConnectionState.IDLE.canTransitionTo(ConnectionState.CONNECTING))
        assertTrue(ConnectionState.IDLE.canTransitionTo(ConnectionState.CLOSED))

        // CONNECTING transitions
        assertTrue(ConnectionState.CONNECTING.canTransitionTo(ConnectionState.CONNECTED))
        assertTrue(ConnectionState.CONNECTING.canTransitionTo(ConnectionState.RECONNECTING))
        assertTrue(ConnectionState.CONNECTING.canTransitionTo(ConnectionState.FAILED))
        assertTrue(ConnectionState.CONNECTING.canTransitionTo(ConnectionState.CLOSED))

        // CONNECTED transitions
        assertTrue(ConnectionState.CONNECTED.canTransitionTo(ConnectionState.RECONNECTING))
        assertTrue(ConnectionState.CONNECTED.canTransitionTo(ConnectionState.CLOSED))
        assertTrue(ConnectionState.CONNECTED.canTransitionTo(ConnectionState.FAILED))

        // RECONNECTING transitions
        assertTrue(ConnectionState.RECONNECTING.canTransitionTo(ConnectionState.CONNECTING))
        assertTrue(ConnectionState.RECONNECTING.canTransitionTo(ConnectionState.CLOSED))
        assertTrue(ConnectionState.RECONNECTING.canTransitionTo(ConnectionState.FAILED))

        // Terminals
        assertFalse(ConnectionState.CLOSED.canTransitionTo(ConnectionState.CONNECTING))
        assertFalse(ConnectionState.FAILED.canTransitionTo(ConnectionState.CONNECTED))
    }

    @Test
    fun `full lifecycle chain`() {
        var state = ConnectionState.IDLE

        fun assertTransition(next: ConnectionState) {
            assertTrue("Should transition from $state to $next", state.canTransitionTo(next))
            state = next
        }

        assertTransition(ConnectionState.CONNECTING)
        assertTransition(ConnectionState.CONNECTED)
        assertTransition(ConnectionState.RECONNECTING)
        assertTransition(ConnectionState.CONNECTING)
        assertTransition(ConnectionState.CONNECTED)
        assertTransition(ConnectionState.CLOSED)
    }
}
