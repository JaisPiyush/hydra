package com.benzenelabs.hydra.host.data.file

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.benzenelabs.hydra.host.data.StorageScope
import com.benzenelabs.hydra.host.data.blob.BlobAccessDeniedException
import com.benzenelabs.hydra.host.data.blob.BlobId
import com.benzenelabs.hydra.host.data.blob.BlobNotFoundException
import com.benzenelabs.hydra.host.data.blob.BlobStore
import com.benzenelabs.hydra.host.data.blob.BlobStoreException
import com.benzenelabs.hydra.host.data.blob.BlobUri
import com.benzenelabs.hydra.contributions.api.ContributionId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * [FileStorage] implementation delegating persistence to [BlobStore].
 */
class FileStorageImpl(
    context: Context,
    private val blobStore: BlobStore
) : FileStorage {

    private val appContext = context.applicationContext

    override suspend fun write(
        scope: StorageScope,
        name: String,
        mimeType: String,
        bytes: ByteArray
    ): FileRef = withContext(Dispatchers.IO) {
        runFileOperation("Failed to write file '$name'") {
            require(name.isNotBlank()) { "File name must not be blank" }
            require(mimeType.isNotBlank()) { "mimeType must not be blank" }
            val blobUri = blobStore.store(scope, bytes, mimeType, name)
            val blobId = blobUri.blobId ?: throw FileStorageException("Stored blob URI is missing blobId")
            toFileRef(blobId, blobUri)
        }
    }

    override suspend fun importFromUri(scope: StorageScope, uri: Uri, nameHint: String?): FileRef =
        withContext(Dispatchers.IO) {
            runFileOperation("Failed to import file from URI: $uri") {
                val discoveredName = resolveDisplayName(uri) ?: nameHint ?: "imported-file"
                val resolvedMimeType = appContext.contentResolver.getType(uri)
                    ?: mimeFromFileName(discoveredName)
                    ?: DEFAULT_MIME_TYPE

                val blobUri = blobStore.importFrom(scope, uri, resolvedMimeType)
                val blobId = blobUri.blobId ?: throw FileStorageException("Imported blob URI is missing blobId")

                // Preserve discovered logical file name by rewriting metadata through store would duplicate bytes,
                // so this implementation falls back to metadata name when available and discoveredName otherwise.
                toFileRef(blobId, blobUri, fallbackName = discoveredName)
            }
        }

    override suspend fun open(scope: StorageScope, blobId: BlobId): InputStream =
        withContext(Dispatchers.IO) {
            runFileOperation("Failed to open file ${blobId.value}") {
                blobStore.openInputStream(scope, blobId)
            }
        }

    override suspend fun readBytes(scope: StorageScope, blobId: BlobId): ByteArray =
        withContext(Dispatchers.IO) {
            runFileOperation("Failed to read file ${blobId.value}") {
                blobStore.readBytes(scope, blobId)
            }
        }

    override suspend fun exportToDownloads(scope: StorageScope, blobId: BlobId, targetName: String): Uri =
        withContext(Dispatchers.IO) {
            runFileOperation("Failed to export file ${blobId.value}") {
                blobStore.exportTo(scope, blobId, targetName)
            }
        }

    override suspend fun getRef(blobId: BlobId): FileRef? = withContext(Dispatchers.IO) {
        runFileOperation("Failed to get file ref for ${blobId.value}") {
            val metadata = blobStore.getMetadata(blobId) ?: return@runFileOperation null
            FileRef(
                blobId = metadata.blobId,
                blobUri = BlobUri.internal(metadata.blobId),
                name = metadata.originalFileName ?: metadata.blobId.value,
                mimeType = metadata.mimeType,
                sizeBytes = metadata.sizeBytes
            )
        }
    }

    override suspend fun delete(scope: StorageScope, blobId: BlobId): Boolean = withContext(Dispatchers.IO) {
        runFileOperation("Failed to delete file ${blobId.value}") {
            blobStore.delete(scope, blobId)
        }
    }

    override suspend fun deleteAll(extensionId: ContributionId) {
        withContext(Dispatchers.IO) {
            runFileOperation("Failed to delete files for extension ${extensionId.value}") {
                blobStore.deleteAll(extensionId)
            }
        }
    }

    override suspend fun listOwned(scope: StorageScope): List<FileRef> = withContext(Dispatchers.IO) {
        runFileOperation("Failed to list files for scope ${scope.label}") {
            blobStore.listOwned(scope).map { metadata ->
                FileRef(
                    blobId = metadata.blobId,
                    blobUri = BlobUri.internal(metadata.blobId),
                    name = metadata.originalFileName ?: metadata.blobId.value,
                    mimeType = metadata.mimeType,
                    sizeBytes = metadata.sizeBytes
                )
            }
        }
    }

    private suspend fun toFileRef(blobId: BlobId, blobUri: BlobUri, fallbackName: String? = null): FileRef {
        val metadata = blobStore.getMetadata(blobId)
            ?: throw FileStorageException("Metadata missing for blob ${blobId.value}")
        return FileRef(
            blobId = blobId,
            blobUri = blobUri,
            name = metadata.originalFileName ?: fallbackName ?: blobId.value,
            mimeType = metadata.mimeType,
            sizeBytes = metadata.sizeBytes
        )
    }

    private fun resolveDisplayName(uri: Uri): String? {
        if (uri.scheme.equals("file", ignoreCase = true)) {
            return uri.lastPathSegment?.substringAfterLast('/')
        }

        val cursor: Cursor = appContext.contentResolver.query(uri, null, null, null, null) ?: return null
        return cursor.use {
            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && it.moveToFirst()) it.getString(index) else null
        }
    }

    private fun mimeFromFileName(name: String): String? {
        val ext = name.substringAfterLast('.', missingDelimiterValue = "")
        if (ext.isBlank()) return null
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase())
    }

    private suspend inline fun <T> runFileOperation(
        message: String,
        crossinline block: suspend () -> T
    ): T =
        try {
            block()
        } catch (e: BlobNotFoundException) {
            throw FileNotFoundException(e.blobId)
        } catch (e: BlobAccessDeniedException) {
            throw FileAccessDeniedException(e.message ?: "File access denied")
        } catch (e: FileNotFoundException) {
            throw e
        } catch (e: FileAccessDeniedException) {
            throw e
        } catch (e: BlobStoreException) {
            throw FileStorageException(message, e)
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            throw FileStorageException(message, e)
        }

    private companion object {
        private const val DEFAULT_MIME_TYPE = "application/octet-stream"
    }
}
