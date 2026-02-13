package com.benzenelabs.hydra.host.data.session

import com.benzenelabs.hydra.contributions.api.ContributionId

/**
 * Domain model for a session - one conversation context on a channel.
 *
 * A session ties together a [channelId] (which channel extension manages it),
 * a [remoteId] (the external platform's conversation identifier), and runtime state.
 *
 * Auth credentials are NOT stored here. They are stored in SecretVault under
 * the channel extension's scope, keyed by a derived key. The session holds only
 * an opaque [authRef] that the channel extension uses to look up its own secrets.
 */
data class Session(
    val id: SessionId,
    val channelId: ContributionId,
    val remoteId: String,
    val displayName: String,
    val state: SessionState,
    val authRef: String?,
    val metadata: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val lastMessageAt: Long?
) {
    init {
        require(remoteId.isNotBlank()) { "Session.remoteId must not be blank" }
        require(displayName.isNotBlank()) { "Session.displayName must not be blank" }
        require(createdAt > 0) { "Session.createdAt must be a positive epoch millis value" }
        require(updatedAt >= createdAt) { "Session.updatedAt must be >= createdAt" }
    }
}
