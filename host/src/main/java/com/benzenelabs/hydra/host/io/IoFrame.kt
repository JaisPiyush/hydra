package com.benzenelabs.hydra.host.io

/**
 * Raw frame delivered between connection transports and channel/runtime layers.
 */
data class IoFrame(
    val connectionId: String,
    val payload: ByteArray,
    val frameType: FrameType,
    val receivedAt: Long
) {
    init {
        require(connectionId.isNotBlank()) { "IoFrame.connectionId must not be blank" }
        require(receivedAt > 0) { "IoFrame.receivedAt must be positive" }
    }

    /** Decodes payload bytes as UTF-8 text. */
    fun payloadAsText(): String = payload.toString(Charsets.UTF_8)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IoFrame) return false
        return connectionId == other.connectionId &&
            payload.contentEquals(other.payload) &&
            frameType == other.frameType &&
            receivedAt == other.receivedAt
    }

    override fun hashCode(): Int {
        var result = connectionId.hashCode()
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + frameType.hashCode()
        result = 31 * result + receivedAt.hashCode()
        return result
    }
}

/** Raw payload encoding type. */
enum class FrameType { BINARY, TEXT }
