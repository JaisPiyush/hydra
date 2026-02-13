package com.benzenelabs.hydra.host.io.fakes

import com.benzenelabs.hydra.contributions.api.ContributionId
import com.benzenelabs.hydra.host.data.session.Session
import com.benzenelabs.hydra.host.data.session.SessionAlreadyExistsException
import com.benzenelabs.hydra.host.data.session.SessionId
import com.benzenelabs.hydra.host.data.session.SessionNotFoundException
import com.benzenelabs.hydra.host.data.session.SessionState
import com.benzenelabs.hydra.host.data.session.SessionStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSessionStore : SessionStore {

    val sessions = mutableMapOf<SessionId, Session>()

    override suspend fun create(session: Session) {
        if (sessions.containsKey(session.id)) {
            throw SessionAlreadyExistsException(session.id)
        }
        sessions[session.id] = session
    }

    override suspend fun findById(id: SessionId): Session? {
        return sessions[id]
    }

    override suspend fun findByRemote(channelId: ContributionId, remoteId: String): Session? {
        return sessions.values
                .filter { it.channelId == channelId && it.remoteId == remoteId }
                .maxByOrNull { it.createdAt }
    }

    override suspend fun findByChannel(channelId: ContributionId): List<Session> {
        return sessions.values.filter { it.channelId == channelId }.sortedByDescending {
            it.updatedAt
        }
    }

    override suspend fun findByState(state: SessionState): List<Session> {
        return sessions.values.filter { it.state == state }
    }

    override suspend fun updateState(id: SessionId, newState: SessionState) {
        val session = sessions[id] ?: throw SessionNotFoundException(id)
        // Assume valid transition for fake, or check canTransitionTo if available
        sessions[id] = session.copy(state = newState, updatedAt = System.currentTimeMillis())
    }

    override suspend fun touchLastMessage(id: SessionId, at: Long) {
        val session = sessions[id] ?: return // or throw?
        // Session usually has a lastMessageAt field? The interface implies it.
        // Assuming implementation details, but for fake checking 'touch' we can just update
        // updatedAt
        sessions[id] = session.copy(updatedAt = at)
    }

    override suspend fun update(session: Session) {
        if (!sessions.containsKey(session.id)) {
            throw SessionNotFoundException(session.id)
        }
        sessions[session.id] = session
    }

    override suspend fun delete(id: SessionId): Boolean {
        return sessions.remove(id) != null
    }

    override suspend fun deleteByChannel(channelId: ContributionId) {
        sessions.entries.removeIf { it.value.channelId == channelId }
    }

    override fun observeByChannel(channelId: ContributionId): Flow<List<Session>> {
        // Simple mock flow
        return MutableStateFlow(sessions.values.filter { it.channelId == channelId })
    }
}
