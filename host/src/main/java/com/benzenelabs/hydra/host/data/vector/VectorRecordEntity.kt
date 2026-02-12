package com.benzenelabs.hydra.host.data.vector

import androidx.room.Entity
import androidx.room.Index

/**
 * Room entity for a single vector record.
 */
@Entity(
    tableName = "vector_records",
    primaryKeys = ["scopeLabel", "collection", "id"],
    indices = [Index(value = ["scopeLabel", "collection"])]
)
data class VectorRecordEntity(
    val scopeLabel: String,
    val collection: String,
    val id: String,
    val vectorJson: String,
    val metadata: String?,
    val createdAt: Long
)
