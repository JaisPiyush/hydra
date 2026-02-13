package com.benzenelabs.hydra.host.channel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory [ChannelRegistry] implementation.
 */
class ChannelRegistryImpl(
    private val scope: CoroutineScope
) : ChannelRegistry {

    private val mutex = Mutex()
    private val registrations = linkedMapOf<ChannelId, ChannelRegistration>()
    private val bridges = linkedMapOf<ChannelId, ChannelBridge>()
    private val bridgeEventJobs = linkedMapOf<ChannelId, Job>()

    private val registrationState = MutableStateFlow<List<ChannelRegistration>>(emptyList())
    private val eventFlow = MutableSharedFlow<ChannelEvent>(extraBufferCapacity = 128)

    override val events: SharedFlow<ChannelEvent> = eventFlow.asSharedFlow()

    override suspend fun register(metadata: ChannelMetadata, bridge: ChannelBridge) {
        val now = System.currentTimeMillis()
        mutex.withLock {
            require(bridge.channelId == metadata.channelId) {
                "Bridge channelId (${bridge.channelId.value}) must match metadata.channelId (${metadata.channelId.value})"
            }
            if (registrations.containsKey(metadata.channelId)) {
                throw ChannelAlreadyRegisteredException(metadata.channelId)
            }

            val registration = ChannelRegistration(
                metadata = metadata,
                state = ChannelState.REGISTERED,
                registeredAt = now,
                lastStateChangeAt = now,
                errorMessage = null
            )
            registrations[metadata.channelId] = registration
            bridges[metadata.channelId] = bridge
            bridgeEventJobs[metadata.channelId] = scope.launch {
                bridge.events.collect { event ->
                    eventFlow.emit(event)
                }
            }
            publishSnapshotLocked()
        }
    }

    override suspend fun unregister(channelId: ChannelId) {
        var bridge: ChannelBridge? = null
        mutex.withLock {
            bridgeEventJobs.remove(channelId)?.cancel()
            bridge = bridges.remove(channelId)
            val removed = registrations.remove(channelId)
            if (removed == null) {
                return@withLock
            }
            publishSnapshotLocked()
        }

        bridge?.close()
    }

    override suspend fun find(channelId: ChannelId): ChannelRegistration? = mutex.withLock {
        registrations[channelId]
    }

    override suspend fun findAll(): List<ChannelRegistration> = mutex.withLock {
        registrations.values.toList()
    }

    override suspend fun findByState(state: ChannelState): List<ChannelRegistration> = mutex.withLock {
        registrations.values.filter { it.state == state }
    }

    override suspend fun getBridge(channelId: ChannelId): ChannelBridge? = mutex.withLock {
        bridges[channelId]
    }

    override suspend fun updateState(
        channelId: ChannelId,
        newState: ChannelState,
        errorMessage: String?
    ) {
        val event: ChannelEvent.StateChanged
        mutex.withLock {
            val current = registrations[channelId] ?: throw ChannelNotFoundException(channelId)
            if (!current.state.canTransitionTo(newState)) {
                throw InvalidChannelTransitionException(channelId, current.state, newState)
            }
            val updated = current.withState(
                newState = newState,
                at = System.currentTimeMillis(),
                errorMessage = errorMessage
            )
            registrations[channelId] = updated
            publishSnapshotLocked()
            event = ChannelEvent.StateChanged(
                channelId = channelId,
                previousState = current.state,
                newState = newState,
                errorMessage = errorMessage
            )
        }
        eventFlow.emit(event)
    }

    override fun observeAll(): Flow<List<ChannelRegistration>> = registrationState.asStateFlow()

    private fun publishSnapshotLocked() {
        registrationState.value = registrations.values.toList()
    }
}
