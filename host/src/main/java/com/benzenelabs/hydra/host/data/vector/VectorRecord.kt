package com.benzenelabs.hydra.host.data.vector

/**
 * Domain model for a vector record.
 */
data class VectorRecord(
    val id: String,
    val vector: FloatArray,
    val metadata: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VectorRecord) return false
        return id == other.id &&
            vector.contentEquals(other.vector) &&
            metadata == other.metadata
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + vector.contentHashCode()
        result = 31 * result + (metadata?.hashCode() ?: 0)
        return result
    }
}
