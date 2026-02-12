package com.benzenelabs.hydra.host.data.file

import com.benzenelabs.hydra.host.data.blob.BlobId
import com.benzenelabs.hydra.host.data.blob.BlobUri

/**
 * A high-level reference to a managed file.
 */
data class FileRef(
    val blobId: BlobId,
    val blobUri: BlobUri,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long
) {
    init {
        require(name.isNotBlank()) { "FileRef name must not be blank" }
        require(mimeType.isNotBlank()) { "FileRef mimeType must not be blank" }
        require(sizeBytes >= 0) { "FileRef sizeBytes must be non-negative" }
    }
}
