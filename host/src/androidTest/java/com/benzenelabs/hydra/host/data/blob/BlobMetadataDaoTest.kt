package com.benzenelabs.hydra.host.data.blob

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.benzenelabs.hydra.host.data.db.PublicDatabase
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlobMetadataDaoTest {

    private lateinit var db: PublicDatabase
    private lateinit var dao: BlobMetadataDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, PublicDatabase::class.java).build()
        dao = db.blobMetadataDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    private fun entity(id: String, scope: String) =
            BlobMetadataEntity(
                    blobId = id,
                    mimeType = "text/plain",
                    sizeBytes = 100,
                    ownerScopeLabel = scope,
                    originalFileName = "orig.txt",
                    createdAt = 123
            )

    @Test
    fun insertAndFindById_returnsEntity() = runTest {
        val e = entity("b1", "agent")
        dao.insert(e)

        val retrieved = dao.findById("b1")
        assertEquals(e, retrieved)
    }

    @Test
    fun findByOwner_returnsOnlyScopeEntries() = runTest {
        dao.insert(entity("b1", "s1"))
        dao.insert(entity("b2", "s1"))
        dao.insert(entity("b3", "s2"))

        val s1Blobs = dao.findByOwner("s1")
        assertEquals(2, s1Blobs.size)

        val s2Blobs = dao.findByOwner("s2")
        assertEquals(1, s2Blobs.size)
    }

    @Test
    fun deleteById_returnsOneForExisting() = runTest {
        dao.insert(entity("b1", "s1"))

        val deleted = dao.deleteById("b1")
        assertEquals(1, deleted)
        assertNull(dao.findById("b1"))
    }

    @Test
    fun deleteById_returnsZeroForMissing() = runTest { assertEquals(0, dao.deleteById("missing")) }

    @Test
    fun deleteByOwner_removesScopeBlobs() = runTest {
        dao.insert(entity("b1", "s1"))
        dao.insert(entity("b2", "s2"))

        dao.deleteByOwner("s1")

        assertNull(dao.findById("b1"))
        assertEquals(entity("b2", "s2"), dao.findById("b2"))
    }

    @Test
    fun countByOwner_tracksInsertsAndDeletes() = runTest {
        assertEquals(0, dao.countByOwner("s1"))

        dao.insert(entity("b1", "s1"))
        assertEquals(1, dao.countByOwner("s1"))

        dao.deleteById("b1")
        assertEquals(0, dao.countByOwner("s1"))
    }

    @Test
    fun originalFileName_canBeNull() = runTest {
        val e =
                BlobMetadataEntity(
                        blobId = "b1",
                        mimeType = "t/p",
                        sizeBytes = 0,
                        ownerScopeLabel = "s",
                        originalFileName = null,
                        createdAt = 1
                )
        dao.insert(e)
        assertNull(dao.findById("b1")?.originalFileName)
    }
}
