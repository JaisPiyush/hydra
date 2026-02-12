package com.benzenelabs.hydra.host.data.blob

import java.util.UUID

/**
 * Opaque, globally unique identifier for a stored binary object.
 */
@JvmInline
value class BlobId(val value: String) {
    init {
        require(value.isNotBlank()) { "BlobId must not be blank" }
        UUID.fromString(value)
    }

    override fun toString(): String = value

    companion object {
        /** Generates a new random [BlobId]. */
        fun generate(): BlobId = BlobId(UUID.randomUUID().toString())
    }
}
