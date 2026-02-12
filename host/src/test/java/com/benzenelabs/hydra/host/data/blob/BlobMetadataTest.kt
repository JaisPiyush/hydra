package com.benzenelabs.hydra.host.data.blob

import com.benzenelabs.hydra.host.data.StorageScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BlobMetadataTest {

    @Test
    fun `data class equality works`() {
        val id = BlobId.generate()
        val m1 =
                BlobMetadata(
                        blobId = id,
                        mimeType = "text/plain",
                        sizeBytes = 100,
                        ownerScope = StorageScope.Agent,
                        originalFileName = "test.txt",
                        createdAt = 123L
                )
        val m2 = m1.copy()

        assertEquals(m1, m2)
    }

    @Test
    fun `originalFileName can be null`() {
        val m =
                BlobMetadata(
                        blobId = BlobId.generate(),
                        mimeType = "image/png",
                        sizeBytes = 0,
                        ownerScope = StorageScope.Agent,
                        originalFileName = null,
                        createdAt = 0L
                )
        assertEquals(null, m.originalFileName)
    }
}
