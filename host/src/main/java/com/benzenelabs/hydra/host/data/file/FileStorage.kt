package com.benzenelabs.hydra.host.data.file

import android.net.Uri
import com.benzenelabs.hydra.host.data.StorageScope
import com.benzenelabs.hydra.host.data.blob.BlobId
import com.benzenelabs.hydra.contributions.api.ContributionId
import java.io.InputStream

/**
 * High-level file abstraction over [com.benzenelabs.hydra.host.data.blob.BlobStore].
 */
interface FileStorage {

    /** Writes bytes as a new file. */
    suspend fun write(
        scope: StorageScope,
        name: String,
        mimeType: String,
        bytes: ByteArray
    ): FileRef

    /** Imports a file from an external URI. */
    suspend fun importFromUri(scope: StorageScope, uri: Uri, nameHint: String? = null): FileRef

    /** Opens an [InputStream] to read a file. */
    suspend fun open(scope: StorageScope, blobId: BlobId): InputStream

    /** Reads a file's full content into a [ByteArray]. */
    suspend fun readBytes(scope: StorageScope, blobId: BlobId): ByteArray

    /** Exports a file to Downloads or MediaStore and returns the external URI. */
    suspend fun exportToDownloads(scope: StorageScope, blobId: BlobId, targetName: String): Uri

    /** Returns the [FileRef] for a stored file, or null if not found. */
    suspend fun getRef(blobId: BlobId): FileRef?

    /** Deletes a file. */
    suspend fun delete(scope: StorageScope, blobId: BlobId): Boolean

    /** Deletes all files owned by [extensionId]. */
    suspend fun deleteAll(extensionId: ContributionId)

    /** Lists all [FileRef] instances owned by [scope]. */
    suspend fun listOwned(scope: StorageScope): List<FileRef>
}

/** Thrown when a file does not exist. */
class FileNotFoundException(blobId: BlobId) :
    Exception("File not found: ${blobId.value}")

/** Thrown when caller scope is not permitted to access the file. */
class FileAccessDeniedException(message: String) : SecurityException(message)

/** Wraps underlying storage failures. */
class FileStorageException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
