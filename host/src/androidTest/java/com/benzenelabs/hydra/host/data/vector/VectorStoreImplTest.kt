package com.benzenelabs.hydra.host.data.vector

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.benzenelabs.hydra.host.data.StorageScope
import com.benzenelabs.hydra.host.data.db.PublicDatabase
import com.benzenelabs.hydra.contributions.api.ContributionId
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VectorStoreImplTest {

    private lateinit var db: PublicDatabase
    private lateinit var store: VectorStoreImpl
    private val extId = ContributionId("test.ext")
    private val scope = StorageScope.Extension(extId)
    private val collection = "test_col"

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, PublicDatabase::class.java).build()
        store = VectorStoreImpl(db.vectorRecordDao())
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun upsertAndGet_persistsRecord() = runTest {
        val record = VectorRecord("id1", floatArrayOf(0.1f, 0.2f), "meta")
        store.upsert(scope, collection, record)

        val retrieved = store.get(scope, collection, "id1")
        assertEquals(record, retrieved)
    }

    @Test
    fun delete_returnsFalseForMissing() = runTest {
        val deleted = store.delete(scope, collection, "missing_id")
        assertNull(store.get(scope, collection, "missing_id"))
        assertEquals(false, deleted)
    }

    @Test
    fun delete_removesExisting() = runTest {
        val record = VectorRecord("id1", floatArrayOf(0.1f, 0.2f), "meta")
        store.upsert(scope, collection, record)

        val deleted = store.delete(scope, collection, "id1")
        assertEquals(true, deleted)
        assertNull(store.get(scope, collection, "id1"))
    }

    @Test
    fun search_sortsByDescendingScore() = runTest {
        // Query: [1.0, 0.0]
        val query = floatArrayOf(1.0f, 0.0f)

        // Exact match (score 1.0)
        val r1 = VectorRecord("id1", floatArrayOf(1.0f, 0.0f), null)
        // Orthogonal (score 0.0)
        val r2 = VectorRecord("id2", floatArrayOf(0.0f, 1.0f), null)
        // Opposite (score -1.0)
        val r3 = VectorRecord("id3", floatArrayOf(-1.0f, 0.0f), null)
        // 45 degrees (score ~0.707)
        val r4 = VectorRecord("id4", floatArrayOf(0.707f, 0.707f), null)

        store.upsert(scope, collection, r1)
        store.upsert(scope, collection, r2)
        store.upsert(scope, collection, r3)
        store.upsert(scope, collection, r4)

        val results = store.search(scope, collection, query, topK = 10)

        assertEquals(4, results.size)
        assertEquals("id1", results[0].record.id)
        assertEquals(1.0f, results[0].score, 0.001f)

        assertEquals("id4", results[1].record.id)
        assertEquals(0.707f, results[1].score, 0.001f)

        assertEquals("id2", results[2].record.id)
        assertEquals(0.0f, results[2].score, 0.001f)

        assertEquals("id3", results[3].record.id)
        assertEquals(-1.0f, results[3].score, 0.001f)
    }

    @Test
    fun search_filtersByMinScore() = runTest {
        val query = floatArrayOf(1.0f, 0.0f)
        val r1 = VectorRecord("id1", floatArrayOf(1.0f, 0.0f), null) // Score 1.0
        val r2 = VectorRecord("id2", floatArrayOf(0.0f, 1.0f), null) // Score 0.0

        store.upsert(scope, collection, r1)
        store.upsert(scope, collection, r2)

        val results = store.search(scope, collection, query, topK = 10, minScore = 0.5f)

        assertEquals(1, results.size)
        assertEquals("id1", results[0].record.id)
    }

    @Test
    fun search_throwsOnDimensionMismatch() = runTest {
        val record = VectorRecord("id1", floatArrayOf(0.1f, 0.2f), null)
        store.upsert(scope, collection, record)

        val query = floatArrayOf(0.1f, 0.2f, 0.3f) // Dim 3 vs Dim 2

        try {
            store.search(scope, collection, query, topK = 1)
            fail("Expected VectorDimensionMismatchException")
        } catch (e: VectorDimensionMismatchException) {
            // Success
        }
    }

    @Test
    fun count_returnsCorrectCount() = runTest {
        assertEquals(0L, store.count(scope, collection))

        store.upsert(scope, collection, VectorRecord("id1", floatArrayOf(0f), null))
        assertEquals(1L, store.count(scope, collection))
    }

    @Test
    fun deleteAll_removesScopeVectors() = runTest {
        store.upsert(scope, collection, VectorRecord("id1", floatArrayOf(0f), null))

        store.deleteAll(extId)

        assertEquals(0L, store.count(scope, collection))
    }

    @Test
    fun cosineSimilarity_logicTest() {
        val v1 = floatArrayOf(1.0f, 0.0f)
        val v2 = floatArrayOf(0.0f, 1.0f)
        val v3 = floatArrayOf(1.0f, 1.0f)

        assertEquals(0.0f, store.cosineSimilarity(v1, v2), 0.0001f)
        assertEquals(1.0f, store.cosineSimilarity(v1, v1), 0.0001f)
        // v1 . v3 = 1 / (1 * sqrt(2)) = 0.7071
        assertEquals(0.70710677f, store.cosineSimilarity(v1, v3), 0.0001f)
    }
}
