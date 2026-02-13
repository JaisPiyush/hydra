package com.benzenelabs.hydra.host.channel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Runtime registry of all loaded channel extensions.
 */
interface ChannelRegistry {

    /**
     * Registers a new channel with its metadata and bridge.
     *
     * @throws ChannelAlreadyRegisteredException if a channel with the same ID is already registered.
     */
    suspend fun register(metadata: ChannelMetadata, bridge: ChannelBridge)

    /**
     * Removes a channel registration and closes its bridge.
     * Idempotent - does not throw if the channel is not registered.
     */
    suspend fun unregister(channelId: ChannelId)

    /** Returns the [ChannelRegistration] for a channel, or null if not registered. */
    suspend fun find(channelId: ChannelId): ChannelRegistration?

    /** Returns all currently registered channels. */
    suspend fun findAll(): List<ChannelRegistration>

    /** Returns all registered channels in a given [ChannelState]. */
    suspend fun findByState(state: ChannelState): List<ChannelRegistration>

    /** Returns the [ChannelBridge] for a channel, or null if not registered. */
    suspend fun getBridge(channelId: ChannelId): ChannelBridge?

    /**
     * Updates the state of a registered channel.
     *
     * @throws ChannelNotFoundException if [channelId] is not registered.
     * @throws InvalidChannelTransitionException if the transition is not permitted.
     */
    suspend fun updateState(
        channelId: ChannelId,
        newState: ChannelState,
        errorMessage: String? = null
    )

    /** Emits the full list of registrations and re-emits on any change. */
    fun observeAll(): Flow<List<ChannelRegistration>>

    /**
     * A [SharedFlow] of all [ChannelEvent]s across all registered channels.
     * Merges the `events` flows of all active bridges.
     */
    val events: SharedFlow<ChannelEvent>
}

class ChannelAlreadyRegisteredException(channelId: ChannelId) :
    IllegalStateException("Channel already registered: ${channelId.value}")

class ChannelNotFoundException(channelId: ChannelId) :
    IllegalStateException("Channel not registered: ${channelId.value}")

class InvalidChannelTransitionException(
    channelId: ChannelId,
    from: ChannelState,
    to: ChannelState
) : IllegalStateException("Invalid state transition for ${channelId.value}: $from -> $to")
