package com.benzenelabs.hydra.host.data.vector

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.benzenelabs.hydra.host.data.db.PublicDatabase
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VectorRecordDaoTest {

    private lateinit var db: PublicDatabase
    private lateinit var dao: VectorRecordDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, PublicDatabase::class.java).build()
        dao = db.vectorRecordDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    private fun entity(scope: String, collection: String, id: String, meta: String? = "meta") =
            VectorRecordEntity(
                    scopeLabel = scope,
                    collection = collection,
                    id = id,
                    vectorJson = "[0.1, 0.2]",
                    metadata = meta,
                    createdAt = System.currentTimeMillis()
            )

    @Test
    fun upsert_insertsAndReplaces() = runTest {
        val e1 = entity("s1", "c1", "id1", "meta1")
        dao.upsert(e1)

        val retrieved = dao.find("s1", "c1", "id1")
        assertEquals(e1, retrieved)

        val e2 = e1.copy(metadata = "meta2")
        dao.upsert(e2) // Should replace

        val retrieved2 = dao.find("s1", "c1", "id1")
        assertEquals("meta2", retrieved2?.metadata)
    }

    @Test fun find_returnsNullForMissing() = runTest { assertNull(dao.find("s1", "c1", "missing")) }

    @Test
    fun findAll_returnsOnlyCollectionEntries() = runTest {
        dao.upsert(entity("s1", "c1", "id1"))
        dao.upsert(entity("s1", "c1", "id2"))
        dao.upsert(entity("s1", "c2", "id1")) // different collection

        val c1 = dao.findAll("s1", "c1")
        assertEquals(2, c1.size)

        val c2 = dao.findAll("s1", "c2")
        assertEquals(1, c2.size)
    }

    @Test
    fun delete_returnsOneForExisting() = runTest {
        dao.upsert(entity("s1", "c1", "id1"))
        assertEquals(1, dao.delete("s1", "c1", "id1"))
        assertNull(dao.find("s1", "c1", "id1"))
    }

    @Test
    fun delete_returnsZeroForMissing() = runTest {
        assertEquals(0, dao.delete("s1", "c1", "missing"))
    }

    @Test
    fun count_tracksChanges() = runTest {
        assertEquals(0L, dao.count("s1", "c1"))

        dao.upsert(entity("s1", "c1", "id1"))
        assertEquals(1L, dao.count("s1", "c1"))

        dao.delete("s1", "c1", "id1")
        assertEquals(0L, dao.count("s1", "c1"))
    }

    @Test
    fun deleteAll_removesScopeEntires() = runTest {
        dao.upsert(entity("s1", "c1", "id1"))
        dao.upsert(entity("s2", "c1", "id1"))

        dao.deleteAll("s1")

        assertNull(dao.find("s1", "c1", "id1"))
        assertNotNull(dao.find("s2", "c1", "id1"))
    }

    @Test
    fun metadata_canBeNull() = runTest {
        val e = entity("s1", "c1", "id1", null)
        dao.upsert(e)
        val retrieved = dao.find("s1", "c1", "id1")
        assertNull(retrieved?.metadata)
    }
}
