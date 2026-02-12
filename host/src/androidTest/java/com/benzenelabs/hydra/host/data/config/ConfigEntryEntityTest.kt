package com.benzenelabs.hydra.host.data.config

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.benzenelabs.hydra.host.data.db.PublicDatabase
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConfigEntryEntityTest {

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

    @Test
    fun insertAndRetrieve_preservesValues() = runTest {
        val entity =
                ConfigEntryEntity(
                        scopeLabel = "agent",
                        key = "theme",
                        value = "dark",
                        isSecret = false,
                        updatedAt = 123456L
                )

        dao.upsert(entity)

        val retrieved = dao.find("agent", "theme")
        assertEquals(entity, retrieved)
    }

    @Test
    fun secretEntry_hasNullValue_andIsSecretTrue() = runTest {
        val entity =
                ConfigEntryEntity(
                        scopeLabel = "ext:foo",
                        key = "api_key",
                        value = null,
                        isSecret = true,
                        updatedAt = 100L
                )

        dao.upsert(entity)

        val retrieved = dao.find("ext:foo", "api_key")
        assertEquals(entity, retrieved)
        assertNull(retrieved?.value)
        assertTrue(retrieved?.isSecret == true)
    }

    @Test
    fun duplicateInsert_replacesExisting() = runTest {
        val entity1 =
                ConfigEntryEntity(
                        scopeLabel = "agent",
                        key = "theme",
                        value = "dark",
                        isSecret = false,
                        updatedAt = 100L
                )
        dao.upsert(entity1)

        val entity2 =
                ConfigEntryEntity(
                        scopeLabel = "agent",
                        key = "theme",
                        value = "light",
                        isSecret = false,
                        updatedAt = 200L
                )
        dao.upsert(entity2)

        val retrieved = dao.find("agent", "theme")
        assertEquals(entity2, retrieved)
        assertEquals("light", retrieved?.value)
    }
}
