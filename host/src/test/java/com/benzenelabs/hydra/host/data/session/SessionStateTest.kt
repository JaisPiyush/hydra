package com.benzenelabs.hydra.host.data.session

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionStateTest {

    @Test
    fun `validate transitions`() {
        // PENDING -> ACTIVE / CLOSED
        assertTrue(SessionState.PENDING.canTransitionTo(SessionState.ACTIVE))
        assertTrue(SessionState.PENDING.canTransitionTo(SessionState.CLOSED))
        assertFalse(SessionState.PENDING.canTransitionTo(SessionState.SUSPENDED))

        // ACTIVE -> SUSPENDED / CLOSED
        assertTrue(SessionState.ACTIVE.canTransitionTo(SessionState.SUSPENDED))
        assertTrue(SessionState.ACTIVE.canTransitionTo(SessionState.CLOSED))
        assertFalse(SessionState.ACTIVE.canTransitionTo(SessionState.PENDING))

        // SUSPENDED -> ACTIVE / CLOSED
        assertTrue(SessionState.SUSPENDED.canTransitionTo(SessionState.ACTIVE))
        assertTrue(SessionState.SUSPENDED.canTransitionTo(SessionState.CLOSED))
        assertFalse(SessionState.SUSPENDED.canTransitionTo(SessionState.PENDING))

        // CLOSED -> none
        assertFalse(SessionState.CLOSED.canTransitionTo(SessionState.ACTIVE))
        assertFalse(SessionState.CLOSED.canTransitionTo(SessionState.PENDING))
        assertFalse(SessionState.CLOSED.canTransitionTo(SessionState.SUSPENDED))
        assertFalse(SessionState.CLOSED.canTransitionTo(SessionState.CLOSED))
    }

    @Test
    fun `all entries exist`() {
        assertEquals(4, SessionState.entries.size)
    }

    @Test
    fun `full transition chain`() {
        var current = SessionState.PENDING

        current = SessionState.ACTIVE
        assertTrue(SessionState.PENDING.canTransitionTo(current))

        val suspended = SessionState.SUSPENDED
        assertTrue(current.canTransitionTo(suspended))
        current = suspended

        val resumed = SessionState.ACTIVE
        assertTrue(current.canTransitionTo(resumed))
        current = resumed

        val closed = SessionState.CLOSED
        assertTrue(current.canTransitionTo(closed))
    }
}
