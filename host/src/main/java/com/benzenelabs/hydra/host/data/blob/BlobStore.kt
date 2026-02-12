package com.benzenelabs.hydra.host.data.blob

import android.net.Uri
import com.benzenelabs.hydra.host.data.StorageScope
import com.benzenelabs.hydra.contributions.api.ContributionId
import java.io.InputStream

/**
 * Storage for binary objects (images, audio, video, arbitrary files).
 */
interface BlobStore {

    /** Stores raw bytes and returns a [BlobUri]. */
    suspend fun store(
        scope: StorageScope,
        data: ByteArray,
        mimeType: String,
        fileName: String? = null
    ): BlobUri

    /** Imports a blob from an external URI into internal storage. */
    suspend fun importFrom(
        scope: StorageScope,
        externalUri: Uri,
        mimeType: String? = null
    ): BlobUri

    /** Exports an internal blob to a user-accessible external URI. */
    suspend fun exportTo(scope: StorageScope, blobId: BlobId, targetFileName: String): Uri

    /** Opens an [InputStream] for the blob's bytes. */
    suspend fun openInputStream(scope: StorageScope, blobId: BlobId): InputStream

    /** Reads full blob content into a [ByteArray]. */
    suspend fun readBytes(scope: StorageScope, blobId: BlobId): ByteArray

    /** Returns [BlobMetadata] for a blob, or null if not found. */
    suspend fun getMetadata(blobId: BlobId): BlobMetadata?

    /** Deletes a blob and its metadata. */
    suspend fun delete(scope: StorageScope, blobId: BlobId): Boolean

    /** Deletes all blobs owned by [extensionId]. */
    suspend fun deleteAll(extensionId: ContributionId)

    /** Lists all [BlobMetadata] entries owned by [scope]. */
    suspend fun listOwned(scope: StorageScope): List<BlobMetadata>
}

/** Thrown when a blob does not exist. */
class BlobNotFoundException(val blobId: BlobId) :
    Exception("Blob not found: ${blobId.value}")

/** Thrown when caller scope is not permitted to access a blob. */
class BlobAccessDeniedException(message: String) : SecurityException(message)

/** Wraps underlying storage failures. */
class BlobStoreException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
