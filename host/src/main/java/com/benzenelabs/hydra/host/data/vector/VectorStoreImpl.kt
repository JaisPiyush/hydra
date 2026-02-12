package com.benzenelabs.hydra.host.data.vector

import com.benzenelabs.hydra.host.data.StorageScope
import com.benzenelabs.hydra.contributions.api.ContributionId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

/**
 * [VectorStore] implementation backed by [VectorRecordDao].
 */
class VectorStoreImpl(
    private val dao: VectorRecordDao
) : VectorStore {

    override suspend fun upsert(
        scope: StorageScope.Extension,
        collection: String,
        record: VectorRecord
    ) {
        withContext(Dispatchers.IO) {
            runVectorOperation("Failed to upsert vector '${record.id}'") {
                validateCollection(collection)
                require(record.id.isNotBlank()) { "Vector record id must not be blank" }
                dao.upsert(
                    VectorRecordEntity(
                        scopeLabel = scope.label,
                        collection = collection,
                        id = record.id,
                        vectorJson = record.vector.toJson(),
                        metadata = record.metadata,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    override suspend fun get(
        scope: StorageScope.Extension,
        collection: String,
        id: String
    ): VectorRecord? = withContext(Dispatchers.IO) {
        runVectorOperation("Failed to read vector '$id'") {
            validateCollection(collection)
            dao.find(scope.label, collection, id)?.toDomain()
        }
    }

    override suspend fun delete(
        scope: StorageScope.Extension,
        collection: String,
        id: String
    ): Boolean = withContext(Dispatchers.IO) {
        runVectorOperation("Failed to delete vector '$id'") {
            validateCollection(collection)
            dao.delete(scope.label, collection, id) > 0
        }
    }

    override suspend fun search(
        scope: StorageScope.Extension,
        collection: String,
        queryVector: FloatArray,
        topK: Int,
        minScore: Float
    ): List<VectorSearchResult> = withContext(Dispatchers.IO) {
        runVectorOperation("Failed to search vectors in '$collection'") {
            validateCollection(collection)
            require(topK >= 0) { "topK must be non-negative" }

            val records = dao.findAll(scope.label, collection).map { it.toDomain() }
            if (records.isEmpty() || topK == 0) {
                return@runVectorOperation emptyList()
            }

            val scored = records.map { record ->
                if (record.vector.size != queryVector.size) {
                    throw VectorDimensionMismatchException(record.vector.size, queryVector.size)
                }
                VectorSearchResult(
                    record = record,
                    score = cosineSimilarity(queryVector, record.vector)
                )
            }

            scored
                .asSequence()
                .filter { it.score >= minScore }
                .sortedByDescending { it.score }
                .take(topK)
                .toList()
        }
    }

    override suspend fun count(
        scope: StorageScope.Extension,
        collection: String
    ): Long = withContext(Dispatchers.IO) {
        runVectorOperation("Failed to count vectors in '$collection'") {
            validateCollection(collection)
            dao.count(scope.label, collection)
        }
    }

    override suspend fun deleteAll(extensionId: ContributionId) {
        withContext(Dispatchers.IO) {
            runVectorOperation("Failed to delete vectors for extension '${extensionId.value}'") {
                dao.deleteAll("ext:${extensionId.value}")
            }
        }
    }

    private fun validateCollection(collection: String) {
        require(collection.isNotBlank()) { "Collection must not be blank" }
        require(collection.length <= 128) { "Collection must be <= 128 characters" }
        require(collection.matches(COLLECTION_PATTERN)) {
            "Collection may only contain alphanumeric characters, underscores, and hyphens"
        }
    }

    private fun VectorRecordEntity.toDomain(): VectorRecord =
        VectorRecord(id = id, vector = vectorJson.toFloatArray(), metadata = metadata)

    private fun FloatArray.toJson(): String = joinToString(separator = ",", prefix = "[", postfix = "]")

    private fun String.toFloatArray(): FloatArray {
        val normalized = trim()
        if (normalized == "[]") return floatArrayOf()
        return normalized
            .removePrefix("[")
            .removeSuffix("]")
            .split(",")
            .map { it.trim().toFloat() }
            .toFloatArray()
    }

    internal fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size)
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom == 0f) 0f else dot / denom
    }

    private suspend inline fun <T> runVectorOperation(
        message: String,
        crossinline block: suspend () -> T
    ): T =
        try {
            block()
        } catch (e: VectorDimensionMismatchException) {
            throw e
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            throw VectorStoreException(message, e)
        }

    private companion object {
        private val COLLECTION_PATTERN = Regex("^[a-zA-Z0-9_-]+$")
    }
}
