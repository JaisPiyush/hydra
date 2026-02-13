package com.benzenelabs.hydra.host.channel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelStateTest {

    @Test
    fun `validate transitions`() {
        // UNREGISTERED
        assertTrue(ChannelState.UNREGISTERED.canTransitionTo(ChannelState.REGISTERED))
        assertFalse(ChannelState.UNREGISTERED.canTransitionTo(ChannelState.CONNECTED))

        // REGISTERED
        assertTrue(ChannelState.REGISTERED.canTransitionTo(ChannelState.CONNECTING))
        assertTrue(ChannelState.REGISTERED.canTransitionTo(ChannelState.UNREGISTERED))

        // CONNECTING
        assertTrue(ChannelState.CONNECTING.canTransitionTo(ChannelState.AUTHENTICATING))
        assertTrue(ChannelState.CONNECTING.canTransitionTo(ChannelState.DISCONNECTED))
        assertTrue(ChannelState.CONNECTING.canTransitionTo(ChannelState.ERROR))

        // AUTHENTICATING
        assertTrue(ChannelState.AUTHENTICATING.canTransitionTo(ChannelState.CONNECTED))
        assertTrue(ChannelState.AUTHENTICATING.canTransitionTo(ChannelState.DISCONNECTED))
        assertTrue(ChannelState.AUTHENTICATING.canTransitionTo(ChannelState.ERROR))

        // CONNECTED
        assertTrue(ChannelState.CONNECTED.canTransitionTo(ChannelState.RECONNECTING))
        assertTrue(ChannelState.CONNECTED.canTransitionTo(ChannelState.DISCONNECTED))
        assertTrue(ChannelState.CONNECTED.canTransitionTo(ChannelState.ERROR))
        assertFalse(ChannelState.CONNECTED.canTransitionTo(ChannelState.CONNECTED))

        // RECONNECTING
        assertTrue(ChannelState.RECONNECTING.canTransitionTo(ChannelState.CONNECTED))
        assertTrue(ChannelState.RECONNECTING.canTransitionTo(ChannelState.DISCONNECTED))
        assertTrue(ChannelState.RECONNECTING.canTransitionTo(ChannelState.ERROR))

        // DISCONNECTED
        assertTrue(ChannelState.DISCONNECTED.canTransitionTo(ChannelState.CONNECTING))
        assertTrue(ChannelState.DISCONNECTED.canTransitionTo(ChannelState.UNREGISTERED))

        // ERROR
        assertTrue(ChannelState.ERROR.canTransitionTo(ChannelState.UNREGISTERED))
        assertFalse(ChannelState.ERROR.canTransitionTo(ChannelState.CONNECTED))
    }
}
