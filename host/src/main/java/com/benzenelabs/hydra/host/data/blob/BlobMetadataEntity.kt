package com.benzenelabs.hydra.host.data.blob

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for blob metadata.
 */
@Entity(tableName = "blob_metadata")
data class BlobMetadataEntity(
    @PrimaryKey val blobId: String,
    val mimeType: String,
    val sizeBytes: Long,
    val ownerScopeLabel: String,
    val originalFileName: String?,
    val createdAt: Long
)
