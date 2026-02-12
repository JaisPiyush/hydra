package com.benzenelabs.hydra.host.data.vector

import com.benzenelabs.hydra.host.data.StorageScope
import com.benzenelabs.hydra.contributions.api.ContributionId

/**
 * Per-extension vector storage for embedding-based retrieval.
 */
interface VectorStore {

    /** Inserts or replaces a vector record by [VectorRecord.id]. */
    suspend fun upsert(scope: StorageScope.Extension, collection: String, record: VectorRecord)

    /** Returns a vector record by [id], or null if not found. */
    suspend fun get(scope: StorageScope.Extension, collection: String, id: String): VectorRecord?

    /** Deletes a vector record by [id]. Returns true if it existed. */
    suspend fun delete(scope: StorageScope.Extension, collection: String, id: String): Boolean

    /**
     * K-nearest-neighbour search using cosine similarity.
     */
    suspend fun search(
        scope: StorageScope.Extension,
        collection: String,
        queryVector: FloatArray,
        topK: Int,
        minScore: Float = 0.0f
    ): List<VectorSearchResult>

    /** Returns the number of records in [collection] for [scope]. */
    suspend fun count(scope: StorageScope.Extension, collection: String): Long

    /** Deletes all vectors across all collections for [extensionId]. */
    suspend fun deleteAll(extensionId: ContributionId)
}

/** Thrown when [StorageScope.Agent] attempts to access vector storage. */
class VectorAccessDeniedException(message: String) : SecurityException(message)

/** Thrown when query vector dimension differs from stored vectors. */
class VectorDimensionMismatchException(expected: Int, actual: Int) :
    IllegalArgumentException("Vector dimension mismatch: expected $expected, got $actual")

/** Wraps underlying storage failures. */
class VectorStoreException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
