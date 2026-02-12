package com.benzenelabs.hydra.host.data.config

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
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
class ConfigEntryDaoTest {

    private lateinit var db: PublicDatabase
    private lateinit var dao: ConfigEntryDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, PublicDatabase::class.java).build()
        dao = db.configEntryDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    private fun entry(scope: String, key: String, value: String? = "val") =
            ConfigEntryEntity(
                    scopeLabel = scope,
                    key = key,
                    value = value,
                    isSecret = value == null,
                    updatedAt = System.currentTimeMillis()
            )

    @Test fun find_returnsNullForMissingKey() = runTest { assertNull(dao.find("scope", "missing")) }

    @Test
    fun observe_emitsUpdates() = runTest {
        val scope = "agent"
        val key = "obs_key"

        dao.observe(scope, key).test {
            assertNull(awaitItem()) // Initial empty state

            val entity = entry(scope, key, "v1")
            dao.upsert(entity)
            assertEquals(entity, awaitItem())

            val entity2 = entry(scope, key, "v2")
            dao.upsert(entity2)
            assertEquals(entity2, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun findAll_returnsOnlyScopeEntries() = runTest {
        dao.upsert(entry("s1", "k1"))
        dao.upsert(entry("s1", "k2"))
        dao.upsert(entry("s2", "k1"))

        val s1Entries = dao.findAll("s1")
        assertEquals(2, s1Entries.size)

        val s2Entries = dao.findAll("s2")
        assertEquals(1, s2Entries.size)
    }

    @Test
    fun deleteAll_removesOnlyScopeEntries() = runTest {
        dao.upsert(entry("s1", "k1"))
        dao.upsert(entry("s2", "k1"))

        dao.deleteAll("s1")

        assertNull(dao.find("s1", "k1"))
        assertEquals(entry("s2", "k1"), dao.find("s2", "k1"))
    }

    @Test
    fun delete_removesSpecificKey() = runTest {
        dao.upsert(entry("s1", "k1"))
        dao.delete("s1", "k1")
        assertNull(dao.find("s1", "k1"))
    }
}
