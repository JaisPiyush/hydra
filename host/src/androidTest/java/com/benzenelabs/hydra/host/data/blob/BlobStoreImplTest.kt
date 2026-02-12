package com.benzenelabs.hydra.host.data.blob

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.benzenelabs.hydra.host.data.StorageScope
import com.benzenelabs.hydra.host.data.db.PublicDatabase
import com.benzenelabs.hydra.contributions.api.ContributionId
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.After
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
class BlobStoreImplTest {

    private lateinit var db: PublicDatabase
    private lateinit var store: BlobStoreImpl
    private val scopeAgent = StorageScope.Agent
    private val ext1 = ContributionId("ext.one")
    private val scopeExt1 = StorageScope.Extension(ext1)
    private val ext2 = ContributionId("ext.two")
    private val scopeExt2 = StorageScope.Extension(ext2)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, PublicDatabase::class.java).build()
        store = BlobStoreImpl(context, db.blobMetadataDao())
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        // We should clean up files created
        runTest {
            // Best effort cleanup through deleteAll for known extensions
            store.deleteAll(ext1)
            store.deleteAll(ext2)
        }
        db.close()
    }

    @Test
    fun store_writesData_andReturnsUri() = runTest {
        val data = "hello world".toByteArray()
        val uri = store.store(scopeExt1, data, "text/plain", "hello.txt")

        assertNotNull(uri.blobId)
        assertTrue(uri.uriString.contains(uri.blobId!!.value))

        val readBack = store.readBytes(scopeExt1, uri.blobId!!)
        assertArrayEquals(data, readBack)
    }

    @Test
    fun delete_removesBlob() = runTest {
        val data = byteArrayOf(1, 2, 3)
        val uri = store.store(scopeExt1, data, "app/bin", null)
        val id = uri.blobId!!

        assertTrue(store.delete(scopeExt1, id))

        assertNull(store.getMetadata(id))
        try {
            store.readBytes(scopeExt1, id)
            fail("Expected BlobNotFoundException")
        } catch (e: BlobNotFoundException) {
            // Success
        }
    }

    @Test
    fun getMetadata_returnsCorrectInfo() = runTest {
        val uri = store.store(scopeExt1, byteArrayOf(), "image/png", "pic.png")

        val meta = store.getMetadata(uri.blobId!!)
        assertNotNull(meta)
        assertEquals("image/png", meta?.mimeType)
        assertEquals("pic.png", meta?.originalFileName)
        assertEquals(0L, meta?.sizeBytes)
    }

    @Test
    fun readBytes_throwsAccessDenied_forWrongExtension() = runTest {
        val uri = store.store(scopeExt1, byteArrayOf(1), "t/p", null)

        try {
            store.readBytes(scopeExt2, uri.blobId!!)
            fail("Expected BlobAccessDeniedException")
        } catch (e: BlobAccessDeniedException) {
            // Success
        }
    }

    @Test
    fun readBytes_agentCanReadExtensionBlob() = runTest {
        val data = byteArrayOf(9, 8, 7)
        val uri = store.store(scopeExt1, data, "t/p", null)

        val readBack = store.readBytes(scopeAgent, uri.blobId!!)
        assertArrayEquals(data, readBack)
    }

    @Test
    fun listOwned_returnsOnlyScopeBlobs() = runTest {
        store.store(scopeExt1, byteArrayOf(), "t/p", null)
        store.store(scopeExt1, byteArrayOf(), "t/p", null)
        store.store(scopeExt2, byteArrayOf(), "t/p", null)

        assertEquals(2, store.listOwned(scopeExt1).size)
        assertEquals(1, store.listOwned(scopeExt2).size)
    }

    @Test
    fun deleteAll_removesTargetExtensionBlobs() = runTest {
        val u1 = store.store(scopeExt1, byteArrayOf(), "t/p", null)
        val u2 = store.store(scopeExt2, byteArrayOf(), "t/p", null)

        store.deleteAll(ext1)

        assertNull(store.getMetadata(u1.blobId!!))
        assertNotNull(store.getMetadata(u2.blobId!!))
    }
}
