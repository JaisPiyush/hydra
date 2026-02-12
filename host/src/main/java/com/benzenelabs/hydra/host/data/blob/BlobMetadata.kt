package com.benzenelabs.hydra.host.data.blob

import com.benzenelabs.hydra.host.data.StorageScope

/**
 * Domain model for blob metadata.
 */
data class BlobMetadata(
    val blobId: BlobId,
    val mimeType: String,
    val sizeBytes: Long,
    val ownerScope: StorageScope,
    val originalFileName: String?,
    val createdAt: Long
)
