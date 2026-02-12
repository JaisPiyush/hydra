package com.benzenelabs.hydra.host.data.file

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.benzenelabs.hydra.host.data.blob.BlobId
import com.benzenelabs.hydra.host.data.blob.BlobUri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FileRefTest {

    private fun validBlobUri() = BlobUri.internal(BlobId.generate())

    @Test
    fun validConstruction() {
        val ref =
                FileRef(
                        blobId = BlobId.generate(),
                        blobUri = validBlobUri(),
                        name = "valid.txt",
                        mimeType = "text/plain",
                        sizeBytes = 100
                )
        assertEquals("valid.txt", ref.name)
    }

    @Test
    fun blankName_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            FileRef(
                    blobId = BlobId.generate(),
                    blobUri = validBlobUri(),
                    name = "",
                    mimeType = "t/p",
                    sizeBytes = 1
            )
        }
    }

    @Test
    fun blankMimeType_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            FileRef(
                    blobId = BlobId.generate(),
                    blobUri = validBlobUri(),
                    name = "n",
                    mimeType = "",
                    sizeBytes = 1
            )
        }
    }

    @Test
    fun negativeSize_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            FileRef(
                    blobId = BlobId.generate(),
                    blobUri = validBlobUri(),
                    name = "n",
                    mimeType = "t/p",
                    sizeBytes = -1
            )
        }
    }

    @Test
    fun zeroSize_valid() {
        FileRef(
                blobId = BlobId.generate(),
                blobUri = validBlobUri(),
                name = "n",
                mimeType = "t/p",
                sizeBytes = 0
        )
    }

    @Test
    fun blobUri_matchesBlobId() {
        val id = BlobId.generate()
        val uri = BlobUri.internal(id)

        val ref = FileRef(id, uri, "n", "t/p", 1)
        assertEquals(id, ref.blobId)
        assertEquals(id, ref.blobUri.blobId)
    }
}
