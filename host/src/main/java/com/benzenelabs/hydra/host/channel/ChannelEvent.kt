package com.benzenelabs.hydra.host.channel

import com.benzenelabs.hydra.host.data.session.SessionId
import com.benzenelabs.hydra.host.data.session.SessionState

/**
 * Events emitted by the [ChannelRegistry] or [ChannelBridge] to internal subscribers.
 */
sealed class ChannelEvent {

    /** A channel's [ChannelState] has changed. */
    data class StateChanged(
        val channelId: ChannelId,
        val previousState: ChannelState,
        val newState: ChannelState,
        val errorMessage: String?
    ) : ChannelEvent()

    /** A new [ChannelMessage] has been received from the platform (inbound). */
    data class MessageReceived(val message: ChannelMessage) : ChannelEvent()

    /** A [ChannelMessage] has been sent successfully to the platform. */
    data class MessageSent(val message: ChannelMessage) : ChannelEvent()

    /** A message send attempt failed. */
    data class MessageSendFailed(
        val sessionId: SessionId,
        val channelId: ChannelId,
        val reason: String,
        val cause: Throwable?
    ) : ChannelEvent()

    /** A new session was created on a channel. */
    data class SessionCreated(val sessionId: SessionId, val channelId: ChannelId) : ChannelEvent()

    /** A session's state changed. */
    data class SessionStateChanged(
        val sessionId: SessionId,
        val channelId: ChannelId,
        val newState: SessionState
    ) : ChannelEvent()

    /** The channel extension is requesting user authentication. */
    data class AuthRequired(
        val channelId: ChannelId,
        val request: AuthRequest
    ) : ChannelEvent()
}
