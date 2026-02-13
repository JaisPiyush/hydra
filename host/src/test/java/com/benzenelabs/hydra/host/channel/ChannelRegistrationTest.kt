package com.benzenelabs.hydra.host.channel

import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelRegistrationTest {

    @Test(expected = IllegalArgumentException::class)
    fun `registeredAt zero throws`() {
        createRegistration(registeredAt = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `lastStateChangeAt before registeredAt throws`() {
        createRegistration(registeredAt = 100, lastStateChangeAt = 99)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `errorMessage on non-ERROR state throws`() {
        createRegistration(state = ChannelState.CONNECTED, errorMessage = "error")
    }

    @Test
    fun `errorMessage on ERROR state allowed`() {
        createRegistration(state = ChannelState.ERROR, errorMessage = "Fatal error")
    }

    @Test
    fun `withState updates state and timestamp`() {
        val original =
                createRegistration(
                        state = ChannelState.REGISTERED,
                        registeredAt = 100,
                        lastStateChangeAt = 100
                )
        val updated = original.withState(ChannelState.CONNECTING, 200)

        assertEquals(ChannelState.CONNECTING, updated.state)
        assertEquals(200L, updated.lastStateChangeAt)
        assertEquals(original.metadata, updated.metadata)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `withState invalid transition throws`() {
        val original = createRegistration(state = ChannelState.REGISTERED)
        // REGISTERED -> CONNECTED is invalid (must go through CONNECTING)
        original.withState(ChannelState.CONNECTED, 200)
    }

    private fun createRegistration(
            state: ChannelState = ChannelState.REGISTERED,
            registeredAt: Long = 1000,
            lastStateChangeAt: Long = 1000,
            errorMessage: String? = null
    ) =
            ChannelRegistration(
                    metadata =
                            ChannelMetadata(
                                    ChannelId("com.example"),
                                    "Display",
                                    "Desc",
                                    null,
                                    true,
                                    ChannelAuthType.NONE,
                                    emptySet()
                            ),
                    state = state,
                    registeredAt = registeredAt,
                    lastStateChangeAt = lastStateChangeAt,
                    errorMessage = errorMessage
            )
}
