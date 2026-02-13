package com.benzenelabs.hydra.host.channel

/**
 * Runtime record of a channel held in the [ChannelRegistry].
 */
data class ChannelRegistration(
    val metadata: ChannelMetadata,
    val state: ChannelState,
    val registeredAt: Long,
    val lastStateChangeAt: Long,
    val errorMessage: String? = null
) {
    init {
        require(registeredAt > 0) { "ChannelRegistration.registeredAt must be positive" }
        require(lastStateChangeAt >= registeredAt) {
            "ChannelRegistration.lastStateChangeAt must be >= registeredAt"
        }
        if (state != ChannelState.ERROR) {
            require(errorMessage == null) {
                "errorMessage must be null when state is not ERROR"
            }
        }
    }

    val channelId: ChannelId
        get() = metadata.channelId

    fun withState(
        newState: ChannelState,
        at: Long,
        errorMessage: String? = null
    ): ChannelRegistration {
        require(state.canTransitionTo(newState)) {
            "Invalid channel state transition: $state -> $newState"
        }
        return copy(
            state = newState,
            lastStateChangeAt = at,
            errorMessage = errorMessage
        )
    }
}
