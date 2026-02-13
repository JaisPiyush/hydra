package com.benzenelabs.hydra.host.data.session

import android.database.sqlite.SQLiteConstraintException
import com.benzenelabs.hydra.contributions.api.ContributionId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * [SessionStore] backed by [SessionDao].
 */
class SessionStoreImpl(
    private val dao: SessionDao
) : SessionStore {

    override suspend fun create(session: Session) {
        withContext(Dispatchers.IO) {
            runSessionOperation("Failed to create session '${session.id.value}'") {
                try {
                    dao.insert(session.toEntity())
                } catch (e: SQLiteConstraintException) {
                    throw SessionAlreadyExistsException(session.id)
                }
            }
        }
    }

    override suspend fun findById(id: SessionId): Session? = withContext(Dispatchers.IO) {
        runSessionOperation("Failed to find session '${id.value}'") {
            dao.findById(id.value)?.toDomain()
        }
    }

    override suspend fun findByRemote(channelId: ContributionId, remoteId: String): Session? =
        withContext(Dispatchers.IO) {
            runSessionOperation("Failed to find session by remote '$remoteId' for '${channelId.value}'") {
                require(remoteId.isNotBlank()) { "remoteId must not be blank" }
                dao.findActiveByRemote(channelId.value, remoteId)?.toDomain()
            }
        }

    override suspend fun findByChannel(channelId: ContributionId): List<Session> =
        withContext(Dispatchers.IO) {
            runSessionOperation("Failed to find sessions for channel '${channelId.value}'") {
                dao.findByChannel(channelId.value).map { it.toDomain() }
            }
        }

    override suspend fun findByState(state: SessionState): List<Session> = withContext(Dispatchers.IO) {
        runSessionOperation("Failed to find sessions in state '$state'") {
            dao.findByState(state.name).map { it.toDomain() }
        }
    }

    override suspend fun updateState(id: SessionId, newState: SessionState) {
        withContext(Dispatchers.IO) {
            runSessionOperation("Failed to update state for session '${id.value}'") {
                val existing = dao.findById(id.value)?.toDomain() ?: throw SessionNotFoundException(id)
                if (!existing.state.canTransitionTo(newState)) {
                    throw InvalidSessionTransitionException(existing.state, newState)
                }
                val now = System.currentTimeMillis()
                val updatedRows = dao.updateState(id.value, newState.name, now)
                if (updatedRows == 0) {
                    throw SessionNotFoundException(id)
                }
            }
        }
    }

    override suspend fun touchLastMessage(id: SessionId, at: Long) {
        withContext(Dispatchers.IO) {
            runSessionOperation("Failed to update last message for session '${id.value}'") {
                require(at > 0) { "at must be positive" }
                val updatedRows = dao.touchLastMessage(id.value, at, System.currentTimeMillis())
                if (updatedRows == 0) {
                    throw SessionNotFoundException(id)
                }
            }
        }
    }

    override suspend fun update(session: Session) {
        withContext(Dispatchers.IO) {
            runSessionOperation("Failed to update session '${session.id.value}'") {
                val updatedRows = dao.update(session.toEntity())
                if (updatedRows == 0) {
                    throw SessionNotFoundException(session.id)
                }
            }
        }
    }

    override suspend fun delete(id: SessionId): Boolean = withContext(Dispatchers.IO) {
        runSessionOperation("Failed to delete session '${id.value}'") {
            dao.deleteById(id.value) > 0
        }
    }

    override suspend fun deleteByChannel(channelId: ContributionId) {
        withContext(Dispatchers.IO) {
            runSessionOperation("Failed to delete sessions for channel '${channelId.value}'") {
                dao.deleteByChannel(channelId.value)
            }
        }
    }

    override fun observeByChannel(channelId: ContributionId): Flow<List<Session>> =
        dao.observeByChannel(channelId.value).map { entities ->
            entities.map { it.toDomain() }
        }

    private fun Session.toEntity(): SessionEntity = SessionEntity(
        id = id.value,
        channelId = channelId.value,
        remoteId = remoteId,
        displayName = displayName,
        state = state.name,
        authRef = authRef,
        metadata = metadata,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastMessageAt = lastMessageAt
    )

    private fun SessionEntity.toDomain(): Session = Session(
        id = SessionId(id),
        channelId = ContributionId(channelId),
        remoteId = remoteId,
        displayName = displayName,
        state = SessionState.valueOf(state),
        authRef = authRef,
        metadata = metadata,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastMessageAt = lastMessageAt
    )

    private suspend inline fun <T> runSessionOperation(
        message: String,
        crossinline block: suspend () -> T
    ): T =
        try {
            block()
        } catch (e: SessionAlreadyExistsException) {
            throw e
        } catch (e: SessionNotFoundException) {
            throw e
        } catch (e: InvalidSessionTransitionException) {
            throw e
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: SessionStoreException) {
            throw e
        } catch (e: Exception) {
            throw SessionStoreException(message, e)
        }
}
