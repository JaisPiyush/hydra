package com.benzenelabs.hydra.host.data.blob.fakes

import android.net.Uri
import com.benzenelabs.hydra.host.data.StorageScope
import com.benzenelabs.hydra.host.data.blob.BlobAccessDeniedException
import com.benzenelabs.hydra.host.data.blob.BlobId
import com.benzenelabs.hydra.host.data.blob.BlobMetadata
import com.benzenelabs.hydra.host.data.blob.BlobNotFoundException
import com.benzenelabs.hydra.host.data.blob.BlobStore
import com.benzenelabs.hydra.host.data.blob.BlobUri
import com.benzenelabs.hydra.contributions.api.ContributionId
import java.io.ByteArrayInputStream
import java.io.InputStream

class FakeBlobStore : BlobStore {

    private val blobs = mutableMapOf<String, ByteArray>()
    private val metadataMap = mutableMapOf<String, BlobMetadata>()

    override suspend fun store(
            scope: StorageScope,
            data: ByteArray,
            mimeType: String,
            fileName: String?
    ): BlobUri {
        val id = BlobId.generate()
        blobs[id.value] = data
        metadataMap[id.value] =
                BlobMetadata(
                        blobId = id,
                        mimeType = mimeType,
                        sizeBytes = data.size.toLong(),
                        ownerScope = scope,
                        originalFileName = fileName,
                        createdAt = System.currentTimeMillis()
                )
        return BlobUri.internal(id)
    }

    override suspend fun importFrom(
            scope: StorageScope,
            externalUri: Uri,
            mimeType: String?
    ): BlobUri {
        // Fake import: create empty or dummy content
        val data = "imported_content".toByteArray()
        return store(scope, data, mimeType ?: "application/octet-stream", null)
    }

    override suspend fun exportTo(
            scope: StorageScope,
            blobId: BlobId,
            targetFileName: String
    ): Uri {
        checkAccess(scope, blobId)
        return Uri.parse("content://fake/export/$targetFileName")
    }

    override suspend fun openInputStream(scope: StorageScope, blobId: BlobId): InputStream {
        checkAccess(scope, blobId)
        val data = blobs[blobId.value] ?: throw BlobNotFoundException(blobId)
        return ByteArrayInputStream(data)
    }

    override suspend fun readBytes(scope: StorageScope, blobId: BlobId): ByteArray {
        checkAccess(scope, blobId)
        return blobs[blobId.value] ?: throw BlobNotFoundException(blobId)
    }

    override suspend fun getMetadata(blobId: BlobId): BlobMetadata? {
        return metadataMap[blobId.value]
    }

    override suspend fun delete(scope: StorageScope, blobId: BlobId): Boolean {
        // In real impl, we check access before delete?
        // Let's assume yes.
        val meta = metadataMap[blobId.value] ?: return false
        checkAccess(scope, blobId)

        blobs.remove(blobId.value)
        metadataMap.remove(blobId.value)
        return true
    }

    override suspend fun deleteAll(extensionId: ContributionId) {
        val keysToRemove =
                metadataMap
                        .filter {
                            val owner = it.value.ownerScope
                            owner is StorageScope.Extension && owner.contributionId == extensionId
                        }
                        .keys

        keysToRemove.forEach {
            blobs.remove(it)
            metadataMap.remove(it)
        }
    }

    override suspend fun listOwned(scope: StorageScope): List<BlobMetadata> {
        return metadataMap.values.filter { it.ownerScope == scope }
    }

    private fun checkAccess(requester: StorageScope, blobId: BlobId) {
        val meta = metadataMap[blobId.value] ?: throw BlobNotFoundException(blobId)

        if (requester is StorageScope.Agent) return

        if (requester != meta.ownerScope) {
            throw BlobAccessDeniedException("Access denied")
        }
    }
}
