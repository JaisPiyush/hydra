package com.benzenelabs.hydra.host.data.file

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.benzenelabs.hydra.host.data.StorageScope
import com.benzenelabs.hydra.host.data.blob.BlobId
import com.benzenelabs.hydra.host.data.blob.fakes.FakeBlobStore
import com.benzenelabs.hydra.contributions.api.ContributionId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FileStorageImplTest {

    private lateinit var blobStore: FakeBlobStore
    private lateinit var storage: FileStorageImpl

    private val scopeAgent = StorageScope.Agent
    private val extId = ContributionId("test.ext")
    private val scopeExt = StorageScope.Extension(extId)
    private val extOther = ContributionId("other.ext")
    private val scopeOther = StorageScope.Extension(extOther)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        blobStore = FakeBlobStore()
        storage = FileStorageImpl(context, blobStore)
    }

    @Test
    fun write_storesFile_andReturnsRef() = runTest {
        val data = "test content".toByteArray()
        val ref = storage.write(scopeExt, "file.txt", "text/plain", data)

        assertNotNull(ref.blobId)
        assertEquals("file.txt", ref.name)
        assertEquals("text/plain", ref.mimeType)
        assertEquals(data.size.toLong(), ref.sizeBytes)

        val readBack = storage.readBytes(scopeExt, ref.blobId!!)
        assertArrayEquals(data, readBack)
    }

    @Test
    fun delete_removesFile() = runTest {
        val ref = storage.write(scopeExt, "del.txt", "t/p", byteArrayOf(1))

        assertTrue(storage.delete(scopeExt, ref.blobId!!))

        assertNull(storage.getRef(ref.blobId!!))
        try {
            storage.readBytes(scopeExt, ref.blobId!!)
            fail("Expected FileNotFoundException")
        } catch (e: FileNotFoundException) {
            // Success (wrapped BlobNotFoundException)
        }
    }

    @Test
    fun readBytes_throwsAccessDenied_forWrongExtension() = runTest {
        val ref = storage.write(scopeExt, "secret.txt", "t/p", byteArrayOf(1))

        try {
            storage.readBytes(scopeOther, ref.blobId!!)
            fail("Expected FileAccessDeniedException")
        } catch (e: FileAccessDeniedException) {
            // Success
        }
    }

    @Test
    fun readBytes_agentCanReadAll() = runTest {
        val ref = storage.write(scopeExt, "user_content.txt", "t/p", byteArrayOf(9))

        val data = storage.readBytes(scopeAgent, ref.blobId!!)
        assertArrayEquals(byteArrayOf(9), data)
    }

    @Test
    fun listOwned_returnsScopeFiles() = runTest {
        storage.write(scopeExt, "f1", "t/p", byteArrayOf())
        storage.write(scopeExt, "f2", "t/p", byteArrayOf())
        storage.write(scopeOther, "f3", "t/p", byteArrayOf())

        val list = storage.listOwned(scopeExt)
        assertEquals(2, list.size)
        assertTrue(list.any { it.name == "f1" })
        assertTrue(list.any { it.name == "f2" })
    }

    @Test
    fun deleteAll_removesTargetExtensionFiles() = runTest {
        val r1 = storage.write(scopeExt, "f1", "t/p", byteArrayOf())
        val r2 = storage.write(scopeOther, "f2", "t/p", byteArrayOf())

        storage.deleteAll(extId)

        assertNull(storage.getRef(r1.blobId!!))
        assertNotNull(storage.getRef(r2.blobId!!))
    }

    @Test
    fun getRef_returnsNullForUnknownBlob() = runTest {
        assertNull(storage.getRef(BlobId.generate()))
    }
}
