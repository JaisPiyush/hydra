package com.benzenelabs.hydra.host.data.vector

/**
 * A single result from a k-nearest-neighbour search.
 */
data class VectorSearchResult(
    val record: VectorRecord,
    val score: Float
)
