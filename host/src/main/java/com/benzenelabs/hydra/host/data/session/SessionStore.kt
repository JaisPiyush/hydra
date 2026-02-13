package com.benzenelabs.hydra.host.data.session

import com.benzenelabs.hydra.contributions.api.ContributionId
import kotlinx.coroutines.flow.Flow

/**
 * Persistent storage for [Session] objects.
 */
interface SessionStore {

    /**
     * Persists a new session. Throws if a session with the same [Session.id] exists.
     *
     * @throws SessionAlreadyExistsException if [session.id] is already stored.
     * @throws SessionStoreException on storage failure.
     */
    suspend fun create(session: Session)

    /** Returns a session by its [SessionId], or null if not found. */
    suspend fun findById(id: SessionId): Session?

    /**
     * Returns the most recent session for a (channelId, remoteId) pair, or null.
     * "Most recent" = highest [Session.createdAt].
     */
    suspend fun findByRemote(channelId: ContributionId, remoteId: String): Session?

    /** Returns all sessions for a channel, ordered by [Session.updatedAt] descending. */
    suspend fun findByChannel(channelId: ContributionId): List<Session>

    /** Returns all sessions in a given [SessionState]. */
    suspend fun findByState(state: SessionState): List<Session>

    /**
     * Updates the state of a session.
     *
     * @throws InvalidSessionTransitionException if the transition is not permitted.
     * @throws SessionNotFoundException if the session does not exist.
     */
    suspend fun updateState(id: SessionId, newState: SessionState)

    /**
     * Records the timestamp of the latest message on a session.
     * Also updates [Session.updatedAt].
     */
    suspend fun touchLastMessage(id: SessionId, at: Long)

    /**
     * Replaces the full session record (e.g., to update metadata or authRef).
     *
     * @throws SessionNotFoundException if the session does not exist.
     */
    suspend fun update(session: Session)

    /**
     * Deletes a session by [SessionId].
     *
     * @return true if deleted, false if not found.
     */
    suspend fun delete(id: SessionId): Boolean

    /** Deletes all sessions for a channel. Called on channel extension uninstall. */
    suspend fun deleteByChannel(channelId: ContributionId)

    /** Emits the current session list for a channel and re-emits on any change. */
    fun observeByChannel(channelId: ContributionId): Flow<List<Session>>
}

class SessionNotFoundException(id: SessionId) :
    Exception("Session not found: ${id.value}")

class SessionAlreadyExistsException(id: SessionId) :
    Exception("Session already exists: ${id.value}")

class InvalidSessionTransitionException(
    from: SessionState,
    to: SessionState
) : IllegalStateException("Invalid session transition: $from -> $to")

class SessionStoreException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
