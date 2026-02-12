package com.benzenelabs.hydra.host.data.blob

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlobUriTest {

    @Test
    fun internalUri_hasCorrectFormat() {
        val id = BlobId.generate()
        val blobUri = BlobUri.internal(id)

        assertEquals("content://com.benzenelabs.hydra.provider/blobs/$id", blobUri.uriString)
        assertEquals(id, blobUri.blobId)
        assertEquals(blobUri.uri.toString(), blobUri.uriString)
    }
}
