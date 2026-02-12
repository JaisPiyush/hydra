package com.benzenelabs.hydra.host.data.blob

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.benzenelabs.hydra.host.data.StorageScope
import com.benzenelabs.hydra.contributions.api.ContributionId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

/**
 * [BlobStore] implementation backed by app-private files and Room metadata.
 */
class BlobStoreImpl(
    context: Context,
    private val metadataDao: BlobMetadataDao
) : BlobStore {

    private val appContext = context.applicationContext

    override suspend fun store(
        scope: StorageScope,
        data: ByteArray,
        mimeType: String,
        fileName: String?
    ): BlobUri = withContext(Dispatchers.IO) {
        runBlobOperation("Failed to store blob") {
            require(mimeType.isNotBlank()) { "mimeType must not be blank" }
            val blobId = BlobId.generate()
            val file = blobFile(blobId)
            file.parentFile?.mkdirs()
            file.outputStream().use { it.write(data) }

            metadataDao.insert(
                BlobMetadataEntity(
                    blobId = blobId.value,
                    mimeType = mimeType,
                    sizeBytes = data.size.toLong(),
                    ownerScopeLabel = scope.label,
                    originalFileName = fileName,
                    createdAt = System.currentTimeMillis()
                )
            )
            BlobUri.internal(blobId)
        }
    }

    override suspend fun importFrom(
        scope: StorageScope,
        externalUri: Uri,
        mimeType: String?
    ): BlobUri = withContext(Dispatchers.IO) {
        runBlobOperation("Failed to import blob from URI: $externalUri") {
            val resolvedMimeType = mimeType ?: appContext.contentResolver.getType(externalUri)
            val stream = appContext.contentResolver.openInputStream(externalUri)
                ?: throw BlobNotFoundException(UNKNOWN_EXTERNAL_BLOB_ID)
            val bytes = stream.use { it.readBytes() }
            store(
                scope = scope,
                data = bytes,
                mimeType = resolvedMimeType ?: DEFAULT_MIME_TYPE,
                fileName = null
            )
        }
    }

    override suspend fun exportTo(scope: StorageScope, blobId: BlobId, targetFileName: String): Uri =
        withContext(Dispatchers.IO) {
            runBlobOperation("Failed to export blob ${blobId.value}") {
                require(targetFileName.isNotBlank()) { "targetFileName must not be blank" }
                val metadata = metadataDao.findById(blobId.value)?.toDomain() ?: throw BlobNotFoundException(blobId)
                enforceBlobAccess(scope, metadata.ownerScope)

                val exportUri = createDownloadUri(targetFileName, metadata.mimeType)
                appContext.contentResolver.openOutputStream(exportUri)?.use { output ->
                    blobFile(blobId).inputStream().use { input -> input.copyTo(output) }
                } ?: throw BlobStoreException("Unable to open export destination URI: $exportUri")
                exportUri
            }
        }

    override suspend fun openInputStream(scope: StorageScope, blobId: BlobId): InputStream =
        withContext(Dispatchers.IO) {
            runBlobOperation("Failed to open blob ${blobId.value}") {
                val metadata = metadataDao.findById(blobId.value)?.toDomain() ?: throw BlobNotFoundException(blobId)
                enforceBlobAccess(scope, metadata.ownerScope)
                val file = blobFile(blobId)
                if (!file.exists()) throw BlobNotFoundException(blobId)
                file.inputStream()
            }
        }

    override suspend fun readBytes(scope: StorageScope, blobId: BlobId): ByteArray =
        withContext(Dispatchers.IO) {
            openInputStream(scope, blobId).use { it.readBytes() }
        }

    override suspend fun getMetadata(blobId: BlobId): BlobMetadata? = withContext(Dispatchers.IO) {
        runBlobOperation("Failed to get metadata for blob ${blobId.value}") {
            metadataDao.findById(blobId.value)?.toDomain()
        }
    }

    override suspend fun delete(scope: StorageScope, blobId: BlobId): Boolean = withContext(Dispatchers.IO) {
        runBlobOperation("Failed to delete blob ${blobId.value}") {
            val metadata = metadataDao.findById(blobId.value)?.toDomain() ?: return@runBlobOperation false
            enforceBlobAccess(scope, metadata.ownerScope)

            val file = blobFile(blobId)
            if (file.exists()) {
                file.delete()
            }
            metadataDao.deleteById(blobId.value)
            true
        }
    }

    override suspend fun deleteAll(extensionId: ContributionId) {
        withContext(Dispatchers.IO) {
            runBlobOperation("Failed to delete blobs for extension ${extensionId.value}") {
                val scopeLabel = "ext:${extensionId.value}"
                metadataDao.findByOwner(scopeLabel).forEach { entity ->
                    val file = blobFile(BlobId(entity.blobId))
                    if (file.exists()) {
                        file.delete()
                    }
                }
                metadataDao.deleteByOwner(scopeLabel)
            }
        }
    }

    override suspend fun listOwned(scope: StorageScope): List<BlobMetadata> = withContext(Dispatchers.IO) {
        runBlobOperation("Failed to list blobs for scope ${scope.label}") {
            metadataDao.findByOwner(scope.label).map { it.toDomain() }
        }
    }

    private fun blobFile(blobId: BlobId): File =
        File(appContext.filesDir, "blobs/${blobId.value}")

    private fun BlobMetadataEntity.toDomain(): BlobMetadata = BlobMetadata(
        blobId = BlobId(blobId),
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        ownerScope = scopeFromLabel(ownerScopeLabel),
        originalFileName = originalFileName,
        createdAt = createdAt
    )

    private fun scopeFromLabel(label: String): StorageScope = when {
        label == StorageScope.Agent.label -> StorageScope.Agent
        label.startsWith("ext:") -> {
            val contributionValue = label.removePrefix("ext:")
            StorageScope.Extension(ContributionId(contributionValue))
        }

        else -> throw BlobStoreException("Unknown scope label in blob metadata: $label")
    }

    private fun enforceBlobAccess(requester: StorageScope, owner: StorageScope) {
        if (requester is StorageScope.Agent) {
            return
        }
        val extRequester = requester as StorageScope.Extension
        val extOwner = owner as? StorageScope.Extension
            ?: throw BlobAccessDeniedException("Extension scope cannot access agent-owned blob")
        if (extRequester.contributionId != extOwner.contributionId) {
            throw BlobAccessDeniedException(
                "Extension '${extRequester.contributionId.value}' cannot access blob owned by '${extOwner.contributionId.value}'"
            )
        }
    }

    private fun createDownloadUri(targetFileName: String, mimeType: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, targetFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        return appContext.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw BlobStoreException("Unable to create destination in MediaStore Downloads")
    }

    private suspend inline fun <T> runBlobOperation(
        message: String,
        crossinline block: suspend () -> T
    ): T =
        try {
            block()
        } catch (e: BlobAccessDeniedException) {
            throw e
        } catch (e: BlobNotFoundException) {
            throw e
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: BlobStoreException) {
            throw e
        } catch (e: Exception) {
            throw BlobStoreException(message, e)
        }

    private companion object {
        private const val DEFAULT_MIME_TYPE = "application/octet-stream"
        private val UNKNOWN_EXTERNAL_BLOB_ID = BlobId("00000000-0000-0000-0000-000000000000")
    }
}
