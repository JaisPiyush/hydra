package com.benzenelabs.hydra.host.data.session

import com.benzenelabs.hydra.contributions.api.ContributionId
import org.junit.Test

class SessionTest {

    @Test(expected = IllegalArgumentException::class)
    fun `blank remoteId throws`() {
        createSession(remoteId = "  ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank displayName throws`() {
        createSession(displayName = "  ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `createdAt zero throws`() {
        createSession(createdAt = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `updatedAt before createdAt throws`() {
        createSession(createdAt = 100, updatedAt = 99)
    }

    @Test
    fun `valid construction`() {
        createSession(authRef = null, lastMessageAt = null)
    }

    private fun createSession(
            remoteId: String = "remote-1",
            displayName: String = "Chat",
            authRef: String? = "auth-ref",
            metadata: String? = null,
            createdAt: Long = 1000,
            updatedAt: Long = 1000,
            lastMessageAt: Long? = 1000
    ) =
            Session(
                    id = SessionId.generate(),
                    channelId = ContributionId("com.example"),
                    remoteId = remoteId,
                    displayName = displayName,
                    state = SessionState.ACTIVE,
                    authRef = authRef,
                    metadata = metadata,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                    lastMessageAt = lastMessageAt
            )
}
